/*
 * Copyright 2015 Brandon Borkholder
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package net.opengrabeso.glg2d.impl.shader;


import java.awt.BasicStroke;
import java.nio.FloatBuffer;

import com.github.opengrabeso.jaagl.GL2GL3;

import net.opengrabeso.glg2d.GLG2DUtils;

import net.opengrabeso.opengl.util.buffers.Buffers;

public class GeometryShaderStrokePipeline extends AbstractShaderPipeline {
    public static final int DRAW_END_NONE = 0;
    public static final int DRAW_END_FIRST = -1;
    public static final int DRAW_END_LAST = 1;
    public static final int DRAW_END_BOTH = 2;

    protected FloatBuffer vBuffer = Buffers.newDirectFloatBuffer(500);

    protected int maxVerticesOut = 32;

    protected int vertCoordLocation;
    protected int vertBeforeLocation;
    protected int vertAfterLocation;
    protected int vertCoordBuffer;

    protected int lineWidthLocation;
    protected int miterLimitLocation;
    protected int joinTypeLocation;
    protected int capTypeLocation;
    protected int drawEndLocation;

    public GeometryShaderStrokePipeline(String shaderDirectory) {
        this(shaderDirectory, "StrokeShader.v", "StrokeShader.g", "StrokeShader.f");
    }

    public GeometryShaderStrokePipeline(String shaderDirectory, String vertexShaderFileName, String geometryShaderFileName, String fragmentShaderFileName) {
        super(shaderDirectory, vertexShaderFileName, geometryShaderFileName, fragmentShaderFileName);
    }

    public void setStroke(GL2GL3 gl, BasicStroke stroke) {
        if (lineWidthLocation >= 0) {
            gl.glUniform1f(lineWidthLocation, stroke.getLineWidth());
        }

        if (miterLimitLocation >= 0) {
            gl.glUniform1f(miterLimitLocation, stroke.getMiterLimit());
        }

        if (joinTypeLocation >= 0) {
            gl.glUniform1i(joinTypeLocation, stroke.getLineJoin());
        }

        if (capTypeLocation >= 0) {
            gl.glUniform1i(capTypeLocation, stroke.getEndCap());
        }
    }

    protected void setDrawEnd(GL2GL3 gl, int drawType) {
        if (drawEndLocation >= 0) {
            gl.glUniform1i(drawEndLocation, drawType);
        }
    }

    protected void bindBuffer(GL2GL3 gl, FloatBuffer vertexBuffer) {
        gl.glEnableVertexAttribArray(vertCoordLocation);
        gl.glEnableVertexAttribArray(vertBeforeLocation);
        gl.glEnableVertexAttribArray(vertAfterLocation);

        if (gl.glIsBuffer(vertCoordBuffer)) {
            gl.glBindBuffer(gl.GL_ARRAY_BUFFER(), vertCoordBuffer);
            gl.glBufferSubData(gl.GL_ARRAY_BUFFER(), 0, Float.BYTES * vertexBuffer.limit(), vertexBuffer);
        } else {
            vertCoordBuffer = GLG2DUtils.genBufferId(gl);

            gl.glBindBuffer(gl.GL_ARRAY_BUFFER(), vertCoordBuffer);
            gl.glBufferData(gl.GL_ARRAY_BUFFER(), Float.BYTES * vertexBuffer.capacity(), vertexBuffer, gl.GL_STREAM_DRAW());
        }

        gl.glVertexAttribPointer(vertCoordLocation, 2, gl.GL_FLOAT(), false, 0, 2 * Float.BYTES);
        gl.glVertexAttribPointer(vertBeforeLocation, 2, gl.GL_FLOAT(), false, 0, 0);
        gl.glVertexAttribPointer(vertAfterLocation, 2, gl.GL_FLOAT(), false, 0, 4 * Float.BYTES);
    }

    public void draw(GL2GL3 gl, FloatBuffer vertexBuffer, boolean close) {
        int pos = vertexBuffer.position();
        int lim = vertexBuffer.limit();
        int numPts = (lim - pos) / 2;

        if (numPts * 2 + 6 > vBuffer.capacity()) {
            vBuffer = Buffers.newDirectFloatBuffer(numPts * 2 + 4);
        }

        vBuffer.clear();

        if (close) {
            vBuffer.put(vertexBuffer.get(lim - 2));
            vBuffer.put(vertexBuffer.get(lim - 1));
            vBuffer.put(vertexBuffer);
            vBuffer.put(vertexBuffer.get(pos));
            vBuffer.put(vertexBuffer.get(pos + 1));
            vBuffer.put(vertexBuffer.get(pos + 2));
            vBuffer.put(vertexBuffer.get(pos + 3));
        } else {
            vBuffer.put(0);
            vBuffer.put(0);
            vBuffer.put(vertexBuffer);
            vBuffer.put(0);
            vBuffer.put(0);
        }

        vBuffer.flip();

        bindBuffer(gl, vBuffer);

        if (close) {
            setDrawEnd(gl, DRAW_END_NONE);
            gl.glDrawArrays(gl.GL_LINES(), 0, numPts + 1);
            gl.glDrawArrays(gl.GL_LINES(), 1, numPts);
        } else if (numPts == 2) {
            setDrawEnd(gl, DRAW_END_BOTH);
            gl.glDrawArrays(gl.GL_LINES(), 0, 2);
        } else {
            setDrawEnd(gl, DRAW_END_NONE);
            gl.glDrawArrays(gl.GL_LINES(), 1, numPts - 2);
            gl.glDrawArrays(gl.GL_LINES(), 2, numPts - 3);

            setDrawEnd(gl, DRAW_END_FIRST);
            gl.glDrawArrays(gl.GL_LINES(), 0, 2);

            setDrawEnd(gl, DRAW_END_LAST);
            gl.glDrawArrays(gl.GL_LINES(), numPts - 2, 2);
        }

        gl.glDisableVertexAttribArray(vertCoordLocation);
        gl.glDisableVertexAttribArray(vertBeforeLocation);
        gl.glDisableVertexAttribArray(vertAfterLocation);

        gl.glBindBuffer(gl.GL_ARRAY_BUFFER(), 0);
    }

    @Override
    protected void setupUniformsAndAttributes(GL2GL3 gl) {
        super.setupUniformsAndAttributes(gl);

        transformLocation = gl.glGetUniformLocation(programId, "u_transform");
        colorLocation = gl.glGetUniformLocation(programId, "u_color");
        lineWidthLocation = gl.glGetUniformLocation(programId, "u_lineWidth");
        miterLimitLocation = gl.glGetUniformLocation(programId, "u_miterLimit");
        joinTypeLocation = gl.glGetUniformLocation(programId, "u_joinType");
        drawEndLocation = gl.glGetUniformLocation(programId, "u_drawEnd");
        capTypeLocation = gl.glGetUniformLocation(programId, "u_capType");

        vertCoordLocation = gl.glGetAttribLocation(programId, "a_vertCoord");
        vertBeforeLocation = gl.glGetAttribLocation(programId, "a_vertBefore");
        vertAfterLocation = gl.glGetAttribLocation(programId, "a_vertAfter");
    }

    @Override
    protected void attachShaders(GL2GL3 gl) {
        super.attachShaders(gl);

        gl.glProgramParameteri(programId, gl.GL_GEOMETRY_INPUT_TYPE(), gl.GL_LINES());
        gl.glProgramParameteri(programId, gl.GL_GEOMETRY_OUTPUT_TYPE(), gl.GL_TRIANGLE_STRIP());
        gl.glProgramParameteri(programId, gl.GL_GEOMETRY_VERTICES_OUT(), maxVerticesOut);
    }

    @Override
    public void delete(GL2GL3 gl) {
        super.delete(gl);

        gl.glDeleteBuffers(new int[]{vertCoordBuffer});
    }
}

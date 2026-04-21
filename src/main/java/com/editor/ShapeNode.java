package com.editor;

import processing.core.*;

public class ShapeNode extends SceneNode {
    public int fillColor;
    public String type;
    public float w, h;

    public ShapeNode(String name, String type, float w, float h, int fillColor) {
        this.name = name;
        this.type = type;
        this.w = w;
        this.h = h;
        this.fillColor = fillColor;
    }

    @Override
    protected void drawNode(PApplet p) {
        p.fill(fillColor);
        p.noStroke();
        if (type.equals("rect")) {
            p.rectMode(PConstants.CENTER);
            p.rect(0, 0, w, h);
        } else if (type.equals("triangle")) {
            p.triangle(0, -h/2, -w/2, h/2, w/2, h/2);
        } else {
            p.ellipse(0, 0, w, h);
        }
    }

    @Override
    protected boolean isInside(float lx, float ly) {
        // Basit bounding box kontrolü
        return lx >= -w/2 && lx <= w/2 && ly >= -h/2 && ly <= h/2;
    }
}
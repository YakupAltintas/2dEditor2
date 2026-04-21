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
        p.stroke(255, 50);
        if (type.equals("rect")) {
            p.box(w, h, w);
        } else if (type.equals("triangle")) {
            drawPrism(p, w, h, w/2);
        } else {
            p.sphere(w/2);
        }
    }

    private void drawPrism(PApplet p, float w, float h, float d) {
        p.beginShape();
        p.vertex(0, -h/2, d); p.vertex(-w/2, h/2, d); p.vertex(w/2, h/2, d);
        p.endShape(PConstants.CLOSE);
        p.beginShape();
        p.vertex(0, -h/2, -d); p.vertex(-w/2, h/2, -d); p.vertex(w/2, h/2, -d);
        p.endShape(PConstants.CLOSE);
        p.beginShape(PConstants.QUAD_STRIP);
        p.vertex(0, -h/2, d); p.vertex(0, -h/2, -d);
        p.vertex(-w/2, h/2, d); p.vertex(-w/2, h/2, -d);
        p.vertex(w/2, h/2, d); p.vertex(w/2, h/2, -d);
        p.vertex(0, -h/2, d); p.vertex(0, -h/2, -d);
        p.endShape();
    }

    @Override
    public void drawHighlight(PApplet p) {
        p.noFill();
        p.stroke(255, 255, 0);
        p.strokeWeight(2);
        if (type.equals("rect")) {
            p.box(w + 5, h + 5, w + 5);
        } else if (type.equals("triangle")) {
            drawPrism(p, w + 5, h + 5, w/2 + 5);
        } else {
            p.sphereDetail(12);
            p.sphere(w/2 + 5);
        }
    }

    public ShapeNode copySelf() {
        return new ShapeNode(name, type, w, h, fillColor);
    }
}
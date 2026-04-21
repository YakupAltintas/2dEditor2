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
            p.box(w, h, 20); // 2D Dikdörtgen yerine 3D Kutu
        } else if (type.equals("triangle")) {
            // Üçgen prizma simülasyonu
            p.beginShape();
            p.vertex(0, -h/2, 10); p.vertex(-w/2, h/2, 10); p.vertex(w/2, h/2, 10);
            p.endShape(PConstants.CLOSE);
            p.beginShape();
            p.vertex(0, -h/2, -10); p.vertex(-w/2, h/2, -10); p.vertex(w/2, h/2, -10);
            p.endShape(PConstants.CLOSE);
        } else {
            p.sphere(w/2); // 2D Daire yerine 3D Küre
        }
    }

    @Override
    protected boolean isInside(float lx, float ly) {
        // Basit bounding box kontrolü
        return lx >= -w/2 && lx <= w/2 && ly >= -h/2 && ly <= h/2;
    }

    @Override
    public SceneNode copy() {
        ShapeNode n = new ShapeNode(name, type, w, h, fillColor);
        n.pos = pos.copy();
        n.rot = rot;
        n.scale = scale.copy();
        n.pivot = pivot.copy();
        n.isAnimating = isAnimating;
        n.rotationSpeed = rotationSpeed;
        for (SceneNode child : children) {
            n.addChild(child.copy());
        }
        return n;
    }
}

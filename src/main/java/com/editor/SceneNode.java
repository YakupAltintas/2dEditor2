package com.editor;

import processing.core.*;
import java.util.*;

public class SceneNode {
    public String name = "Node";
    public PVector pos = new PVector(0, 0, 0);
    public float rot = 0;
    public PVector scale = new PVector(1, 1, 1);
    public PVector pivot = new PVector(0, 0, 0);
    public boolean isAnimating = false;
    public float rotationSpeed = 0.02f;
    
    public List<SceneNode> children = new ArrayList<>();
    public SceneNode parent = null;

    public void addChild(SceneNode child) {
        child.parent = this;
        children.add(child);
    }

    public PMatrix3D getLocalMatrix() {
        PMatrix3D m = new PMatrix3D();
        m.translate(pos.x, pos.y, pos.z);
        m.rotateZ(rot);
        m.scale(scale.x, scale.y, scale.z);
        m.translate(-pivot.x, -pivot.y, -pivot.z);
        return m;
    }

    public PMatrix3D getGlobalMatrix() {
        PMatrix3D res = getLocalMatrix();
        if (parent != null) {
            res.preApply(parent.getGlobalMatrix());
        }
        return res;
    }

    public void update() {
        if (isAnimating) rot += rotationSpeed;
        for (SceneNode child : children) child.update();
    }

    public void display(PApplet p) {
        p.pushMatrix();
        p.applyMatrix(getLocalMatrix());
        drawNode(p);
        
        // Draw pivot
        p.stroke(255, 0, 0);
        p.point(pivot.x, pivot.y, pivot.z);
        
        for (SceneNode child : children) {
            child.display(p);
        }
        p.popMatrix();
    }

    protected void drawNode(PApplet p) {}
    public void drawHighlight(PApplet p) {}

    public boolean contains(PApplet p, float mx, float my) {
        float sx = p.screenX(0, 0, 0);
        float sy = p.screenY(0, 0, 0);
        if (this instanceof ShapeNode) {
            ShapeNode sn = (ShapeNode)this;
            float threshold = (sn.w + sn.h) / 4.0f * scale.x;
            return p.dist(mx, my, sx, sy) < threshold;
        }
        return false;
    }

    public SceneNode copy() {
        SceneNode n = (this instanceof ShapeNode) ? 
            ((ShapeNode)this).copySelf() : new SceneNode();
        n.name = this.name;
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
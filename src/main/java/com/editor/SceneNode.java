package com.editor;

import processing.core.*;
import java.util.*;

public class SceneNode {
    public String name = "Node";
    public PVector pos = new PVector(0, 0);
    public float rot = 0;
    public PVector scale = new PVector(1, 1);
    public PVector pivot = new PVector(0, 0);
    public boolean isAnimating = false;
    public float rotationSpeed = 0.02f;
    
    public List<SceneNode> children = new ArrayList<>();
    public SceneNode parent = null;

    public void addChild(SceneNode child) {
        child.parent = this;
        children.add(child);
    }

    public PMatrix2D getLocalMatrix() {
        PMatrix2D m = new PMatrix2D();
        m.translate(pos.x, pos.y);
        m.rotate(rot);
        m.scale(scale.x, scale.y);
        m.translate(-pivot.x, -pivot.y);
        return m;
    }

    public PMatrix2D getGlobalMatrix() {
        PMatrix2D res = getLocalMatrix();
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
        
        p.fill(255, 0, 0);
        p.ellipse(pivot.x, pivot.y, 5, 5);
        
        for (SceneNode child : children) {
            child.display(p);
        }
        p.popMatrix();
    }

    protected void drawNode(PApplet p) {}

    public boolean contains(PApplet p, float mx, float my) {
        PMatrix2D inv = getGlobalMatrix();
        inv.invert();
        float localX = inv.multX(mx, my);
        float localY = inv.multY(mx, my);
        return isInside(localX, localY);
    }

    protected boolean isInside(float lx, float ly) { return false; }
    
    public SceneNode copy() {
        SceneNode n = new SceneNode();
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
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
        
        // Pivot noktasını göster
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
    
    // State for Undo
    public NodeState getState() {
        return new NodeState(pos.copy(), rot, scale.copy(), pivot.copy(), isAnimating);
    }

    public void setState(NodeState s) {
        this.pos = s.pos.copy();
        this.rot = s.rot;
        this.scale = s.scale.copy();
        this.pivot = s.pivot.copy();
        this.isAnimating = s.isAnimating;
    }

    public static class NodeState {
        PVector pos, scale, pivot;
        float rot;
        boolean isAnimating;
        NodeState(PVector p, float r, PVector s, PVector pv, boolean a) {
            pos=p; rot=r; scale=s; pivot=pv; isAnimating=a;
        }
    }
}

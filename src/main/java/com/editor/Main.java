package com.editor;

import processing.core.*;
import java.util.*;

public class Main extends PApplet {
    SceneNode root;
    SceneNode selectedNode;
    Stack<Map<SceneNode, SceneNode.NodeState>> undoStack = new Stack<>();
    
    public static void main(String[] args) {
        PApplet.main("com.editor.Main");
    }

    public void settings() {
        size(800, 600);
    }

    public void setup() {
        root = new SceneNode();
        root.name = "Root";

        // Hiyerarşi: Güneş -> Dünya -> Ay
        ShapeNode sun = new ShapeNode("Gunes", "ellipse", 100, 100, color(255, 200, 0));
        sun.pos.set(width/2, height/2);
        sun.isAnimating = true;
        sun.rotationSpeed = 0.01f;
        
        ShapeNode earth = new ShapeNode("Dunya", "ellipse", 40, 40, color(0, 150, 255));
        earth.pos.set(180, 0);
        earth.isAnimating = true;
        earth.rotationSpeed = 0.03f;
        
        ShapeNode moon = new ShapeNode("Ay", "rect", 15, 15, color(200));
        moon.pos.set(50, 0);
        moon.isAnimating = true;
        moon.rotationSpeed = 0.08f;

        root.addChild(sun);
        sun.addChild(earth);
        earth.addChild(moon);
        
        selectedNode = sun;
        saveState();
    }

    public void draw() {
        background(20);
        drawGrid();
        
        root.update();
        root.display(this);
        
        drawSelectionHighlight();
        drawUI();
    }

    void drawGrid() {
        stroke(50);
        for(int i=0; i<width; i+=40) line(i,0,i,height);
        for(int i=0; i<height; i+=40) line(0,i,width,i);
        stroke(0, 255, 0); line(width/2, 0, width/2, height); // Y Ekseni
        stroke(255, 0, 0); line(0, height/2, width, height/2); // X Ekseni
    }

    void drawSelectionHighlight() {
        if (selectedNode != null && selectedNode instanceof ShapeNode) {
            PMatrix2D m = selectedNode.getGlobalMatrix();
            pushMatrix();
            applyMatrix(m);
            noFill();
            stroke(255, 255, 0);
            strokeWeight(2);
            rect(0, 0, ((ShapeNode)selectedNode).w + 10, ((ShapeNode)selectedNode).h + 10);
            popMatrix();
        }
    }

    void drawUI() {
        fill(255);
        textSize(12);
        text("SECILI: " + (selectedNode != null ? selectedNode.name : "Yok"), 20, 25);
        text("KONTROLLER: Oklar (Tasin), W/S (Olcek), A/D (Don), P (Pivot Set), L (Animasyon), U (Undo)", 20, 45);
        
        if (selectedNode != null) {
            PMatrix2D m = selectedNode.getLocalMatrix();
            text("MATRIS (3x3):", 20, 80);
            text(String.format("[%.2f, %.2f, %.2f]", m.m00, m.m01, m.m02), 20, 100);
            text(String.format("[%.2f, %.2f, %.2f]", m.m10, m.m11, m.m12), 20, 120);
            text("[0.00, 0.00, 1.00]", 20, 140);
        }
    }

    public void mousePressed() {
        // Hit test starting from children (deepest first)
        selectedNode = findNode(root, mouseX, mouseY);
    }

    SceneNode findNode(SceneNode current, float mx, float my) {
        for (int i = current.children.size() - 1; i >= 0; i--) {
            SceneNode found = findNode(current.children.get(i), mx, my);
            if (found != null) return found;
        }
        if (current.contains(this, mx, my)) return current;
        return null;
    }

    public void keyPressed() {
        if (selectedNode == null) return;
        
        boolean changed = false;
        if (keyCode == UP) { selectedNode.pos.y -= 5; changed = true; }
        if (keyCode == DOWN) { selectedNode.pos.y += 5; changed = true; }
        if (keyCode == LEFT) { selectedNode.pos.x -= 5; changed = true; }
        if (keyCode == RIGHT) { selectedNode.pos.x += 5; changed = true; }
        
        if (key == 'w' || key == 'W') { selectedNode.scale.add(0.1f, 0.1f); changed = true; }
        if (key == 's' || key == 'S') { selectedNode.scale.sub(0.1f, 0.1f); changed = true; }
        if (key == 'a' || key == 'A') { selectedNode.rot -= 0.1f; changed = true; }
        if (key == 'd' || key == 'D') { selectedNode.rot += 0.1f; changed = true; }
        
        if (key == 'p' || key == 'P') {
            PMatrix2D inv = selectedNode.getGlobalMatrix();
            inv.invert();
            selectedNode.pivot.set(inv.multX(mouseX, mouseY), inv.multY(mouseX, mouseY));
            changed = true;
        }

        if (key == 'l' || key == 'L') { selectedNode.isAnimating = !selectedNode.isAnimating; changed = true; }
        
        if (key == 'u' || key == 'U') undo();

        if (changed) saveState();
    }

    void saveState() {
        Map<SceneNode, SceneNode.NodeState> state = new HashMap<>();
        captureState(root, state);
        undoStack.push(state);
        if (undoStack.size() > 10) undoStack.remove(0);
    }

    void captureState(SceneNode n, Map<SceneNode, SceneNode.NodeState> map) {
        map.put(n, n.getState());
        for (SceneNode c : n.children) captureState(c, map);
    }

    void undo() {
        if (undoStack.size() > 1) {
            undoStack.pop(); // Current state
            Map<SceneNode, SceneNode.NodeState> prevState = undoStack.peek();
            for (Map.Entry<SceneNode, SceneNode.NodeState> entry : prevState.entrySet()) {
                entry.getKey().setState(entry.getValue());
            }
        }
    }

    public void mouseDragged() {
        if (selectedNode != null && mouseButton == LEFT) {
            selectedNode.pos.add(mouseX - pmouseX, mouseY - pmouseY);
        }
    }
    
    public void mouseReleased() {
        if (mouseButton == LEFT) saveState();
    }
}

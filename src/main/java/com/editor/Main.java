package com.editor;

import processing.core.*;
import java.util.*;

public class Main extends PApplet {
    SceneNode root;
    SceneNode selectedNode;
    Stack<SceneNode> undoStack = new Stack<>();
    
    public static void main(String[] args) {
        PApplet.main("com.editor.Main");
    }

    public void settings() {
        size(800, 600, P3D);
        smooth(8);
    }

    public void setup() {
        pixelDensity(displayDensity());
        root = new SceneNode();
        root.name = "Root";

        ShapeNode sun = new ShapeNode("Sun", "ellipse", 100, 100, color(255, 200, 0));
        sun.pos.set(width/2, height/2, 0);
        sun.isAnimating = true;
        sun.rotationSpeed = 0.01f;
        
        ShapeNode earth = new ShapeNode("Earth", "ellipse", 40, 40, color(0, 150, 255));
        earth.pos.set(180, 0, 0);
        earth.isAnimating = true;
        earth.rotationSpeed = 0.03f;
        
        ShapeNode moon = new ShapeNode("Moon", "rect", 15, 15, color(200));
        moon.pos.set(50, 0, 0);
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
        lights();
        
        pushMatrix();
        translate(width/2, height/2, -200);
        rotateX(PI/6);
        translate(-width/2, -height/2, 200);
        
        drawGrid();
        root.update();
        root.display(this);
        drawSelectionHighlight();
        popMatrix();
        
        drawUI();
    }

    void drawGrid() {
        stroke(50);
        for(int i=0; i<=width; i+=40) line(i,0,0, i,height,0);
        for(int i=0; i<=height; i+=40) line(0,i,0, width,i,0);
        stroke(0, 255, 0); line(width/2, 0, 0, width/2, height, 0);
        stroke(255, 0, 0); line(0, height/2, 0, width, height/2, 0);
    }

    void drawSelectionHighlight() {
        if (selectedNode != null) {
            pushMatrix();
            applyMatrix(selectedNode.getGlobalMatrix());
            selectedNode.drawHighlight(this);
            popMatrix();
        }
    }

    void drawUI() {
        hint(PConstants.DISABLE_DEPTH_TEST);
        fill(255);
        textSize(12);
        text("SELECTED: " + (selectedNode != null ? selectedNode.name : "None"), 20, 25);
        text("CONTROLS: Arrows (Move), W/S (Uniform Scale), A/D (Rotate), P (Set Pivot), L (Anim), U (Undo), DEL (Remove)", 20, 45);
        text("ADD: 1 (Square), 2 (Circle), 3 (Triangle)", 20, 65);
        
        if (selectedNode != null) {
            PMatrix3D m = selectedNode.getLocalMatrix();
            text("3D MATRIX (Rotation/Scale part):", 20, 95);
            text(String.format("[%.2f, %.2f, %.2f]", m.m00, m.m01, m.m02), 20, 115);
            text(String.format("[%.2f, %.2f, %.2f]", m.m10, m.m11, m.m12), 20, 135);
            text(String.format("[%.2f, %.2f, %.2f]", m.m20, m.m21, m.m22), 20, 155);
        }
        hint(PConstants.ENABLE_DEPTH_TEST);
    }

    public void mousePressed() {
        pushMatrix();
        translate(width/2, height/2, -200);
        rotateX(PI/6);
        translate(-width/2, -height/2, 200);
        selectedNode = findNode(root, mouseX, mouseY);
        popMatrix();
    }

    SceneNode findNode(SceneNode current, float mx, float my) {
        pushMatrix();
        applyMatrix(current.getLocalMatrix());
        SceneNode found = null;
        for (int i = current.children.size() - 1; i >= 0; i--) {
            found = findNode(current.children.get(i), mx, my);
            if (found != null) break;
        }
        if (found == null && current.contains(this, mx, my)) found = current;
        popMatrix();
        return found;
    }

    public void keyPressed() {
        if (key == '1') addNewShape("rect");
        if (key == '2') addNewShape("ellipse");
        if (key == '3') addNewShape("triangle");

        if (key == DELETE || key == BACKSPACE || keyCode == DELETE || keyCode == BACKSPACE) {
            if (selectedNode != null && selectedNode.parent != null) {
                selectedNode.parent.children.remove(selectedNode);
                selectedNode = null;
                saveState();
                return; // Prevent further processing for this key press
            }
        }

        if (selectedNode == null) {
            if (key == 'u' || key == 'U') undo();
            return;
        }
        
        boolean changed = false;
        if (keyCode == UP) { selectedNode.pos.y -= 5; changed = true; }
        if (keyCode == DOWN) { selectedNode.pos.y += 5; changed = true; }
        if (keyCode == LEFT) { selectedNode.pos.x -= 5; changed = true; }
        if (keyCode == RIGHT) { selectedNode.pos.x += 5; changed = true; }
        
        if (key == 'w' || key == 'W') { selectedNode.scale.add(0.05f, 0.05f, 0.05f); changed = true; }
        if (key == 's' || key == 'S') { selectedNode.scale.sub(0.05f, 0.05f, 0.05f); changed = true; }
        if (key == 'a' || key == 'A') { selectedNode.rot -= 0.1f; changed = true; }
        if (key == 'd' || key == 'D') { selectedNode.rot += 0.1f; changed = true; }
        
        if (key == 'p' || key == 'P') {
            pushMatrix();
            applyMatrix(selectedNode.getGlobalMatrix());
            PMatrix3D inv = selectedNode.getGlobalMatrix();
            inv.invert();
            selectedNode.pivot.set(inv.multX(mouseX, mouseY, 0), inv.multY(mouseX, mouseY, 0), 0);
            popMatrix();
            changed = true;
        }

        if (key == 'l' || key == 'L') { selectedNode.isAnimating = !selectedNode.isAnimating; changed = true; }
        if (key == 'u' || key == 'U') undo();
        if (changed) saveState();
    }

    void addNewShape(String type) {
        int c = color(random(100, 255), random(100, 255), random(100, 255));
        ShapeNode newNode = new ShapeNode("New " + type, type, 50, 50, c);
        SceneNode parentNode = (selectedNode != null) ? selectedNode : root;
        
        pushMatrix();
        applyMatrix(parentNode.getGlobalMatrix());
        PMatrix3D inv = parentNode.getGlobalMatrix();
        inv.invert();
        newNode.pos.set(inv.multX(mouseX, mouseY, 0), inv.multY(mouseX, mouseY, 0), 0);
        popMatrix();

        parentNode.addChild(newNode);
        if (parentNode != root) {
            newNode.isAnimating = true;
            newNode.rotationSpeed = random(0.03f, 0.08f);
        }
        selectedNode = newNode;
        saveState();
    }

    void saveState() {
        undoStack.push(root.copy());
        if (undoStack.size() > 15) undoStack.remove(0);
    }

    void undo() {
        if (undoStack.size() > 1) {
            undoStack.pop();
            root = undoStack.peek().copy();
            selectedNode = null;
        }
    }

    public void mouseDragged() {
        if (selectedNode != null && mouseButton == LEFT) {
            selectedNode.pos.x += (mouseX - pmouseX);
            selectedNode.pos.y += (mouseY - pmouseY);
        }
    }
    
    public void mouseReleased() {
        if (mouseButton == LEFT) saveState();
    }
}

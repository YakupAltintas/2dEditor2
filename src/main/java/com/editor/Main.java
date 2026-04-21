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
        sun.pos.set(width/2, height/2);
        sun.isAnimating = true;
        sun.rotationSpeed = 0.01f;
        
        ShapeNode earth = new ShapeNode("Earth", "ellipse", 40, 40, color(0, 150, 255));
        earth.pos.set(180, 0);
        earth.isAnimating = true;
        earth.rotationSpeed = 0.03f;
        
        ShapeNode moon = new ShapeNode("Moon", "rect", 15, 15, color(200));
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
        lights(); // 3D Işıklandırma
        
        // Sahneyi 3D göstermek için hafif bir eğim
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
        for(int i=0; i<width; i+=40) line(i,0,i,height);
        for(int i=0; i<height; i+=40) line(0,i,width,i);
        stroke(0, 255, 0); line(width/2, 0, width/2, height);
        stroke(255, 0, 0); line(0, height/2, width, height/2);
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
        text("SELECTED: " + (selectedNode != null ? selectedNode.name : "None"), 20, 25);
        text("CONTROLS: Arrows (Move), W/S (Scale), A/D (Rotate), P (Set Pivot), L (Anim), U (Undo), DEL (Remove)", 20, 45);
        text("ADD: 1 (Square), 2 (Circle), 3 (Triangle)", 20, 65);
        
        if (selectedNode != null) {
            PMatrix2D m = selectedNode.getLocalMatrix();
            text("MATRIX (3x3):", 20, 95);
            text(String.format("[%.2f, %.2f, %.2f]", m.m00, m.m01, m.m02), 20, 115);
            text(String.format("[%.2f, %.2f, %.2f]", m.m10, m.m11, m.m12), 20, 135);
            text("[0.00, 0.00, 1.00]", 20, 155);
        }
    }

    public void mousePressed() {
        // Match the transforms applied in draw()
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
        // Search children first (those on top)
        for (int i = current.children.size() - 1; i >= 0; i--) {
            found = findNode(current.children.get(i), mx, my);
            if (found != null) break;
        }
        
        // If not found in children, check this node
        if (found == null && current.contains(this, mx, my)) {
            found = current;
        }
        
        popMatrix();
        return found;
    }

    public void keyPressed() {
        if (key == '1') addNewShape("rect");
        if (key == '2') addNewShape("ellipse");
        if (key == '3') addNewShape("triangle");

        if (keyCode == DELETE || keyCode == BACKSPACE) {
            if (selectedNode != null && selectedNode.parent != null) {
                selectedNode.parent.children.remove(selectedNode);
                selectedNode = null;
                saveState();
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

    void addNewShape(String type) {
        int c = color(random(100, 255), random(100, 255), random(100, 255));
        ShapeNode newNode = new ShapeNode("New " + type, type, 50, 50, c);
        
        // Eğer bir nesne seçiliyse yeni nesneyi ona bağla, değilse root'a ekle
        SceneNode parentNode = (selectedNode != null) ? selectedNode : root;
        
        // Fare koordinatlarını ebeveynin yerel sistemine çevir
        PMatrix2D inv = parentNode.getGlobalMatrix();
        inv.invert();
        float lx = inv.multX(mouseX, mouseY);
        float ly = inv.multY(mouseX, mouseY);
        
        newNode.pos.set(lx, ly);
        parentNode.addChild(newNode);
        
        // Eğer bir ebeveyne eklendiyse otomatik yörünge hareketi ver
        if (parentNode != root) {
            newNode.isAnimating = true;
            newNode.rotationSpeed = random(0.03f, 0.08f);
            newNode.name = "Satellite of " + parentNode.name;
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
            selectedNode.pos.add(mouseX - pmouseX, mouseY - pmouseY);
        }
    }
    
    public void mouseReleased() {
        if (mouseButton == LEFT) saveState();
    }
}

package com.editor;

import processing.core.*;
import java.util.*;

public class Main extends PApplet {
    SceneNode root;
    SceneNode selectedNode;
    Stack<SceneNode> undoStack = new Stack<>();
    
    // Camera/Navigation variables
    float camX = 0, camY = 0, camZ = 0;
    boolean[] keyState = new boolean[1024];
    
    public static void main(String[] args) {
        PApplet.main("com.editor.Main");
    }

    public void settings() {
        size(800, 600, P3D);
        smooth(8);
    }

    public void setup() {
        surface.setResizable(true);
        pixelDensity(displayDensity());
        resetScene();
    }

    void resetScene() {
        camX = 0; camY = 0; camZ = 0;
        root = new SceneNode();
        root.name = "Root";

        ShapeNode sun = new ShapeNode("Sun", "ellipse", 100, 100, color(255, 200, 0));
        sun.pos.set(0, 0, 0);
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
        
        // Handle Scene Navigation (Unity-style)
        if (mousePressed && mouseButton == LEFT) {
            float speed = 5.0f;
            if (keyState['w'] || keyState['W']) camZ += speed;
            if (keyState['s'] || keyState['S']) camZ -= speed;
            if (keyState['a'] || keyState['A']) camX += speed;
            if (keyState['d'] || keyState['D']) camX -= speed;
            if (keyState['q'] || keyState['Q']) camY += speed;
            if (keyState['e'] || keyState['E']) camY -= speed;
        }

        pushMatrix();
        // Apply Camera and Scene Transform (Centered at 0,0,0)
        translate(width/2 + camX, height/2 + camY, camZ);
        rotateX(PI/6);
        
        drawGrid();
        root.update();
        root.display(this);
        drawSelectionHighlight();
        popMatrix();
        
        drawUI();
    }

    void drawGrid() {
        stroke(50);
        int range = 2000; // Geniş kapsamlı ızgara
        int step = 40;
        
        // Yatay ve Dikey çizgiler (Orijin merkezli)
        for(int i = -range; i <= range; i += step) {
            line(i, -range, 0, i, range, 0);
            line(-range, i, 0, range, i, 0);
        }
        
        // Eksen çizgileri
        strokeWeight(2);
        stroke(0, 255, 0); line(0, -range, 0, 0, range, 0); // Y Ekseni (Yeşil)
        stroke(255, 0, 0); line(-range, 0, 0, range, 0, 0); // X Ekseni (Kırmızı)
        strokeWeight(1);
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
        text("NAV: Hold Left-Click + WASD (Horizontal), QE (Vertical)", 20, 45);
        text("EDIT: Arrows (Move), W/S (Scale), A/D (Rotate), P (Pivot), L (Anim), U (Undo), DEL (Delete), R (Reset)", 20, 65);
        text("ADD: 1 (Square), 2 (Circle), 3 (Triangle)", 20, 85);
        
        if (selectedNode != null) {
            PMatrix3D m = selectedNode.getLocalMatrix();
            text("3D MATRIX (Rotation/Scale part):", 20, 115);
            text(String.format("[%.2f, %.2f, %.2f]", m.m00, m.m01, m.m02), 20, 135);
            text(String.format("[%.2f, %.2f, %.2f]", m.m10, m.m11, m.m12), 20, 155);
            text(String.format("[%.2f, %.2f, %.2f]", m.m20, m.m21, m.m22), 20, 175);
        }
        hint(PConstants.ENABLE_DEPTH_TEST);
    }

    public void mousePressed() {
        // Match the transforms applied in draw()
        pushMatrix();
        translate(width/2 + camX, height/2 + camY, camZ);
        rotateX(PI/6);
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
        if (key < 1024) keyState[key] = true;

        if (key == '1') addNewShape("rect");
        if (key == '2') addNewShape("ellipse");
        if (key == '3') addNewShape("triangle");
        if (key == 'r' || key == 'R') resetScene();

        if (key == DELETE || key == BACKSPACE || keyCode == DELETE || keyCode == BACKSPACE) {
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
        // Only allow editing transforms if NOT in navigation mode (Left Click held)
        boolean isNavigating = mousePressed && mouseButton == LEFT;
        
        if (keyCode == UP) { selectedNode.pos.y -= 5; changed = true; }
        if (keyCode == DOWN) { selectedNode.pos.y += 5; changed = true; }
        if (keyCode == LEFT) { selectedNode.pos.x -= 5; changed = true; }
        if (keyCode == RIGHT) { selectedNode.pos.x += 5; changed = true; }
        
        if (!isNavigating) {
            if (key == 'w' || key == 'W') { selectedNode.scale.add(0.05f, 0.05f, 0.05f); changed = true; }
            if (key == 's' || key == 'S') { selectedNode.scale.sub(0.05f, 0.05f, 0.05f); changed = true; }
            if (key == 'a' || key == 'A') { selectedNode.rot -= 0.1f; changed = true; }
            if (key == 'd' || key == 'D') { selectedNode.rot += 0.1f; changed = true; }
        }
        
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

    public void keyReleased() {
        if (key < 1024) keyState[key] = false;
    }

    void addNewShape(String type) {
        int c = color(random(100, 255), random(100, 255), random(100, 255));
        ShapeNode newNode = new ShapeNode("New " + type, type, 50, 50, c);
        SceneNode parentNode = (selectedNode != null) ? selectedNode : root;
        
        pushMatrix();
        // Must include camera transforms for correct placement
        translate(width/2 + camX, height/2 + camY, camZ);
        rotateX(PI/6);
        
        applyMatrix(parentNode.getGlobalMatrix());
        
        PMatrix3D totalM = new PMatrix3D();
        getMatrix(totalM);
        totalM.invert();
        
        PVector localPos = new PVector(totalM.multX(mouseX, mouseY, 0), totalM.multY(mouseX, mouseY, 0), 0);
        // Limit the distance from parent to keep satellites close
        if (parentNode != root) {
            localPos.limit(120);
        }
        newNode.pos.set(localPos);
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
        if (selectedNode != null && mouseButton == LEFT && !keyState['w'] && !keyState['a'] && !keyState['s'] && !keyState['d']) {
            pushMatrix();
            // Apply same scene transforms as in draw()
            translate(width/2 + camX, height/2 + camY, -200 + camZ);
            rotateX(PI/6);
            translate(-width/2, -height/2, 200);
            
            // Apply parent's global matrix to get to the coordinate system where selectedNode.pos lives
            if (selectedNode.parent != null) {
                applyMatrix(selectedNode.parent.getGlobalMatrix());
            }

            PMatrix3D totalM = new PMatrix3D();
            getMatrix(totalM);
            totalM.invert();
            
            // Project current and previous mouse positions into local space
            float x1 = totalM.multX(pmouseX, pmouseY, 0);
            float y1 = totalM.multY(pmouseX, pmouseY, 0);
            float x2 = totalM.multX(mouseX, mouseY, 0);
            float y2 = totalM.multY(mouseX, mouseY, 0);
            
            // Update position based on local delta
            selectedNode.pos.x += (x2 - x1);
            selectedNode.pos.y += (y2 - y1);
            
            popMatrix();
        }
    }
    
    public void mouseReleased() {
        if (mouseButton == LEFT) saveState();
    }
}

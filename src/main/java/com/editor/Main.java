package com.editor;

import processing.core.*;
import java.util.*;

public class Main extends PApplet {
    SceneNode root;
    SceneNode selectedNode;
    Stack<SceneNode> undoStack = new Stack<>();
    
    // Kamera Degiskenleri (Fly-Cam)
    float camX = 0, camY = 0, camZ = 500;
    float camRotY = 0; 
    float camRotX = 0; 
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
        camX = 0; camY = 0; camZ = 500;
        camRotX = 0; camRotY = 0;
        root = new SceneNode();
        root.name = "Root";

        ShapeNode sun = new ShapeNode("Gunes", "ellipse", 100, 100, color(255, 200, 0));
        sun.pos.set(0, 0, 0);
        sun.isAnimating = true;
        sun.rotationSpeed = 0.01f;
        
        ShapeNode earth = new ShapeNode("Dunya", "ellipse", 40, 40, color(0, 150, 255));
        earth.pos.set(180, 0, 0);
        earth.isAnimating = true;
        earth.rotationSpeed = 0.03f;
        
        ShapeNode moon = new ShapeNode("Ay", "rect", 15, 15, color(200));
        moon.pos.set(50, 0, 0);
        moon.isAnimating = true;
        moon.rotationSpeed = 0.08f;

        root.addChild(sun);
        sun.addChild(earth);
        earth.addChild(moon);
        selectedNode = sun;
        saveState();
    }

    void applyCamera() {
        float dx = sin(camRotY) * cos(camRotX);
        float dy = sin(camRotX);
        float dz = -cos(camRotY) * cos(camRotX);
        camera(camX, camY, camZ, camX + dx, camY + dy, camZ + dz, 0, 1, 0);
    }

    void updateCamera() {
        if (mousePressed && mouseButton == RIGHT) {
            float speed = 8.0f;
            float fx = sin(camRotY) * cos(camRotX);
            float fy = sin(camRotX);
            float fz = -cos(camRotY) * cos(camRotX);
            float rx = cos(camRotY);
            float rz = sin(camRotY);

            if (keyState['w'] || keyState['W']) { camX += fx * speed; camY += fy * speed; camZ += fz * speed; }
            if (keyState['s'] || keyState['S']) { camX -= fx * speed; camY -= fy * speed; camZ -= fz * speed; }
            if (keyState['a'] || keyState['A']) { camX -= rx * speed; camZ -= rz * speed; }
            if (keyState['d'] || keyState['D']) { camX += rx * speed; camZ += rz * speed; }
            if (keyState['q'] || keyState['Q']) camY -= speed;
            if (keyState['e'] || keyState['E']) camY += speed;
        }
    }

    public void draw() {
        background(20);
        updateCamera();
        
        pushMatrix();
        applyCamera();
        lights();
        drawGrid();
        root.update();
        root.display(this);
        drawSelectionHighlight();
        popMatrix();
        
        drawUI();
    }

    void drawGrid() {
        stroke(50);
        int range = 2000, step = 40;
        for(int i = -range; i <= range; i += step) {
            line(i, -range, 0, i, range, 0);
            line(-range, i, 0, range, i, 0);
        }
        strokeWeight(2);
        stroke(0, 255, 0); line(0, -range, 0, 0, range, 0);
        stroke(255, 0, 0); line(-range, 0, 0, range, 0, 0);
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
        camera(); 
        hint(PConstants.DISABLE_DEPTH_TEST);
        fill(255); textSize(12);
        text("SECILI: " + (selectedNode != null ? selectedNode.name : "Yok"), 20, 25);
        text("GEZINTI: Sag Tik + WASD (Bakis Yonu), QE (Yukari/Asagi) | Sag Tik + Fare (Bakis Acisi)", 20, 45);
        text("DUZENLE: Sol Tik (Tasi), W/S (Olcek), A/D (Don), P (Pivot), L (Anim), U (Geri), DEL (Sil), R (Sifirla)", 20, 65);
        text("EKLE: 1 (Kare), 2 (Daire), 3 (Ucgen)", 20, 85);
        hint(PConstants.ENABLE_DEPTH_TEST);
    }

    public void mousePressed() {
        // Nesne secimi artik SOL TIK ile yapilir
        if (mouseButton == LEFT) {
            pushMatrix();
            applyCamera();
            selectedNode = findNode(root, mouseX, mouseY);
            popMatrix();
        }
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
                return;
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
        
        if (!(mousePressed && mouseButton == RIGHT)) {
            if (key == 'w' || key == 'W') { selectedNode.scale.add(0.05f, 0.05f, 0.05f); changed = true; }
            if (key == 's' || key == 'S') { selectedNode.scale.sub(0.05f, 0.05f, 0.05f); changed = true; }
            if (key == 'a' || key == 'A') { selectedNode.rot -= 0.1f; changed = true; }
            if (key == 'd' || key == 'D') { selectedNode.rot += 0.1f; changed = true; }
        }
        
        if (key == 'p' || key == 'P') {
            pushMatrix();
            applyCamera();
            PMatrix3D inv = new PMatrix3D();
            getMatrix(inv);
            inv.preApply(selectedNode.getGlobalMatrix());
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
        ShapeNode newNode = new ShapeNode("Yeni " + type, type, 50, 50, c);
        SceneNode parentNode = (selectedNode != null) ? selectedNode : root;
        
        pushMatrix();
        applyCamera();
        applyMatrix(parentNode.getGlobalMatrix());
        
        PMatrix3D totalM = new PMatrix3D();
        getMatrix(totalM); 
        totalM.invert();
        
        newNode.pos.set(totalM.multX(mouseX, mouseY, 0), totalM.multY(mouseX, mouseY, 0), totalM.multZ(mouseX, mouseY, 0));
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
        if (mouseButton == RIGHT) {
            camRotY -= (mouseX - pmouseX) * 0.005f;
            camRotX += (mouseY - pmouseY) * 0.005f;
            camRotX = constrain(camRotX, -HALF_PI + 0.01f, HALF_PI - 0.01f);
        } else if (mouseButton == LEFT && selectedNode != null) {
            pushMatrix();
            applyCamera();
            if (selectedNode.parent != null) applyMatrix(selectedNode.parent.getGlobalMatrix());
            PMatrix3D totalM = new PMatrix3D();
            getMatrix(totalM); totalM.invert();
            float x1 = totalM.multX(pmouseX, pmouseY, 0), y1 = totalM.multY(pmouseX, pmouseY, 0);
            float x2 = totalM.multX(mouseX, mouseY, 0), y2 = totalM.multY(mouseX, mouseY, 0);
            selectedNode.pos.x += (x2 - x1); selectedNode.pos.y += (y2 - y1);
            popMatrix();
        }
    }

    public void mouseReleased() {
        if (mouseButton == LEFT) saveState();
    }
}

package com.editor;

import processing.core.*;
import processing.event.MouseEvent;
import processing.opengl.PGraphics3D;
import java.util.*;

public class Main extends PApplet {
    SceneNode root;
    SceneNode selectedNode;
    Stack<SceneNode> undoStack = new Stack<>();
    
    // Fly-Cam Değişkenleri
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

    // Ekran piksellerini Dünya Z=0 koordinatlarına çevirir (Hassas unprojection)
    PVector getMouseWorld(float mx, float my) {
        PGraphics3D p3d = (PGraphics3D) g;
        PMatrix3D combined = p3d.projection.get();
        combined.apply(p3d.modelview);
        combined.invert();

        float x = map(mx, 0, width, -1, 1);
        float y = map(my, 0, height, 1, -1);

        // Near point (NDC z = -1) with W-division
        float wn = combined.m30 * x + combined.m31 * y + combined.m32 * (-1) + combined.m33;
        float nx = (combined.m00 * x + combined.m01 * y + combined.m02 * (-1) + combined.m03) / wn;
        float ny = (combined.m10 * x + combined.m11 * y + combined.m12 * (-1) + combined.m13) / wn;
        float nz = (combined.m20 * x + combined.m21 * y + combined.m22 * (-1) + combined.m23) / wn;

        // Far point (NDC z = 1) with W-division
        float wf = combined.m30 * x + combined.m31 * y + combined.m32 * 1 + combined.m33;
        float fx = (combined.m00 * x + combined.m01 * y + combined.m02 * 1 + combined.m03) / wf;
        float fy = (combined.m10 * x + combined.m11 * y + combined.m12 * 1 + combined.m13) / wf;
        float fz = (combined.m20 * x + combined.m21 * y + combined.m22 * 1 + combined.m23) / wf;

        // Ray-Plane (Z=0) Intersection
        float t = -nz / (fz - nz);
        return new PVector(nx + t * (fx - nx), ny + t * (fy - ny), 0);
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
            float fx = sin(camRotY) * cos(camRotX), fy = sin(camRotX), fz = -cos(camRotY) * cos(camRotX);
            float rx = cos(camRotY), rz = sin(camRotY);
            float ux = -sin(camRotY) * sin(camRotX), uy = cos(camRotX), uz = cos(camRotY) * sin(camRotX);

            if (keyState['w'] || keyState['W']) { camX += fx * speed; camY += fy * speed; camZ += fz * speed; }
            if (keyState['s'] || keyState['S']) { camX -= fx * speed; camY -= fy * speed; camZ -= fz * speed; }
            if (keyState['a'] || keyState['A']) { camX -= rx * speed; camZ -= rz * speed; }
            if (keyState['d'] || keyState['D']) { camX += rx * speed; camZ += rz * speed; }
            if (keyState['q'] || keyState['Q']) { camX -= ux * speed; camY -= uy * speed; camZ -= uz * speed; }
            if (keyState['e'] || keyState['E']) { camX += ux * speed; camY += uy * speed; camZ += uz * speed; }
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
        stroke(50); int range = 2000, step = 40;
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
        camera(); hint(PConstants.DISABLE_DEPTH_TEST);
        fill(255); textSize(12);
        text("SECILI: " + (selectedNode != null ? selectedNode.name : "Yok"), 20, 25);
        text("GEZINTI: Sag Tik + WASD (Bakis), Q/E (Yukari/Asagi), Tekerlek (Zoom) | Sag Tik + Fare (Don)", 20, 45);
        text("DUZENLE: Sol Tik (Sec/Tasi), W/S (Olcek), A/D (Don), P (Pivot), L (Anim), U (Geri), DEL (Sil)", 20, 65);
        text("EKLE: 1 (Kare), 2 (Daire), 3 (Ucgen) | R (Sifirla)", 20, 85);
        hint(PConstants.ENABLE_DEPTH_TEST);
    }

    public void mousePressed() {
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
            pushMatrix(); applyCamera();
            PVector wPos = getMouseWorld(mouseX, mouseY);
            PMatrix3D inv = selectedNode.getGlobalMatrix();
            inv.invert();
            selectedNode.pivot.set(inv.multX(wPos.x, wPos.y, 0), inv.multY(wPos.x, wPos.y, 0), 0);
            popMatrix(); changed = true;
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
        
        pushMatrix(); applyCamera();
        PVector wPos = getMouseWorld(mouseX, mouseY);
        
        PMatrix3D inv = parentNode.getGlobalMatrix();
        inv.invert();
        newNode.pos.set(inv.multX(wPos.x, wPos.y, 0), inv.multY(wPos.x, wPos.y, 0), 0);
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
            camRotY += (mouseX - pmouseX) * 0.005f;
            camRotX += (mouseY - pmouseY) * 0.005f;
            camRotX = constrain(camRotX, -HALF_PI + 0.01f, HALF_PI - 0.01f);
        } else if (mouseButton == LEFT && selectedNode != null) {
            pushMatrix(); applyCamera();
            
            PVector wCurr = getMouseWorld(mouseX, mouseY);
            PVector wPrev = getMouseWorld(pmouseX, pmouseY);
            
            PMatrix3D inv = new PMatrix3D();
            if (selectedNode.parent != null) inv = selectedNode.parent.getGlobalMatrix();
            inv.invert();
            
            float dx = inv.multX(wCurr.x, wCurr.y, 0) - inv.multX(wPrev.x, wPrev.y, 0);
            float dy = inv.multY(wCurr.x, wCurr.y, 0) - inv.multY(wPrev.x, wPrev.y, 0);
            
            selectedNode.pos.x += dx; selectedNode.pos.y += dy;
            popMatrix();
        }
    }

    public void mouseReleased() {
        if (mouseButton == LEFT) saveState();
    }

    public void mouseWheel(MouseEvent event) {
        float e = event.getCount();
        float fx = sin(camRotY) * cos(camRotX), fy = sin(camRotX), fz = -cos(camRotY) * cos(camRotX);
        camX += fx * e * 30; camY += fy * e * 30; camZ += fz * e * 30;
    }
}
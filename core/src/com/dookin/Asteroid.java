package com.dookin;

import com.badlogic.gdx.math.EarClippingTriangulator;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.physics.box2d.Body;
import com.badlogic.gdx.physics.box2d.BodyDef;
import com.badlogic.gdx.physics.box2d.Joint;
import com.badlogic.gdx.physics.box2d.PolygonShape;
import com.badlogic.gdx.physics.box2d.Shape;
import com.badlogic.gdx.physics.box2d.World;
import com.badlogic.gdx.physics.box2d.joints.WeldJoint;
import com.badlogic.gdx.physics.box2d.joints.WeldJointDef;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.ShortArray;
import com.dookin.states.PlayState;

import java.util.Arrays;
import java.util.HashMap;

/**
 * Created by David on 3/19/2018.
 */

public class Asteroid {
    private int fragments;
    public Body fragment1;
    public Body fragment2;
    private World world;
    private Vector3 pos;
    private float scale;
    private float maxStress = 0; //todo for collisions
    private Body base;

    public Array<WeldJoint> joints = new Array<WeldJoint>();
    public HashMap<Body, Array<Joint>> jointmap = new HashMap<Body, Array<Joint>>(); //technically not needed

    private static final int NONE = -1;
    private static final int RAND0 = 0;
    private static final int RAND1 = 1;
    private static final int PRESET1 = 2;

    EarClippingTriangulator zrt = new EarClippingTriangulator();


    public WeldJoint z;
    public boolean fragmented = false;

    public Asteroid(Vector3 pos, World world, int generator, float scale, Body base) {

        this.scale = scale;
        this.pos = pos;
        this.world = world;
        this.base = base;

        /*plotter: https://www.librec.net/datagen.html*/
        switch (generator) {
            case RAND0: {
                generateRand();
                break;
            }
            case RAND1: {
                generateRand1();
                break;
            }
            case PRESET1: {
                presetSimpleAst();
                break;
            }
            case NONE: {

                break;
            }
        }
/*

        Body f1 = createFragment1();
        fragment1 = f1;

        Body f2 = createFragment2();
        fragment2 = f2;


        float[] vertices = new float[]{
              0,0,1,-1, 2, -.5f,2.5f,.5f,1.5f, 1
        };






        WeldJointDef jdef = new WeldJointDef();
        jdef.bodyA = f1;
        jdef.bodyB = f2;
        jdef.collideConnected = false;
        jdef.localAnchorA.set(0.0f,0);
        jdef.localAnchorB.set(0.0f,0);
       // jdef.enableLimit = true;
       // jdef.lowerAngle = 0;
       // jdef.upperAngle = 0;
        z = (WeldJoint)world.createJoint(jdef);

        fragment1.setUserData(this);
        fragment2.setUserData(this);


        */
        //world.destroyJoint(z);
    }

    private void generateRand1() {

        float[] vertices = randConvexCircle(0.2f * 10, -1f, 14, 2f, true);
        Array<Body> bA = selfTriangulate(vertices); //creates bodies as well.


        //thread the bodies together
        WeldJointDef jdef = new WeldJointDef();
        for (int i = 1; i < bA.size; i++) {


            jdef.bodyA = bA.get(i - 1);
            jdef.bodyB = bA.get(i);
            jdef.collideConnected = false;
            jdef.localAnchorA.set(0.0f, 0); //???? figure this out it somehow works tho lmao
            jdef.localAnchorB.set(0.0f, 0);
            WeldJoint tmp = (WeldJoint) world.createJoint(jdef);
            joints.add(tmp);
        }
    }
    public Array<Body> selfTriangulate(float[] vertices) {
        Array<Body> result = new Array<Body>();
        PolygonShape poly = new PolygonShape();
        float[] tmp = new float[6];
        for (int i = 0; i <= vertices.length - 6; i+=6) {
            tmp[0] = vertices[i];
            tmp[1] = vertices[i+1];
            tmp[2] = vertices[i+2];
            tmp[3] = vertices[i+3];
            tmp[4] = vertices[i+4];
            tmp[5] = vertices[i+5];
            poly.set(tmp);
            result.add(createFromShape(poly));
        }
        poly.dispose();
        return result;
    }
    public Body createFromShape(Shape s) {
        Body pBody;

        BodyDef def = new BodyDef();
        def.type = BodyDef.BodyType.DynamicBody;
        System.out.println(pos.x + " " + pos.y);
        def.position.set(pos.x, pos.y);
        pBody = world.createBody(def);
        pBody.createFixture(s, 3.0f);
        pBody.setUserData(this);
        return pBody;
    }



    //breaks off one body
    public void separateChunk(Body base) {
        for (int i = 0; i < joints.size; i++) {
            if (joints.get(i).getBodyA().equals(base) || joints.get(i).getBodyB().equals(base)) {
                PlayState.destroyedJoints.add(joints.get(i));
                joints.removeIndex(i);
                i--;
            }
        }
        //this line would allow for pieces of the same asteroid, after they collide, to be able to destroy the original
        //base.setUserData(new Asteroid(new Vector3(), world, -1, 1, base)); //create new asteroid for the broken off chunk with same body

/*
NOTES:
multiple unwelded bodies may touch at the same point, they collide which breaks the asteroid
//MindGame.destroyedJoints.addAll(jointmap.get(base)); //can't use jointmap cause bodies share joints and it doesn't make sense to do this
 //base.setUserData(new Asteroid(new Vector3(), world, -1, 1, base)); //create new asteroid for the broken off chunk with same body
*/

    }

    public void presetSimpleAst() {
        Body pBody1, pBody2, pBody3;
        float[] coords = new float[] {
                0,0,3.65f,1.6f, 2.75f, 3.55f, .15f, 4.4f
        };
        BodyDef def = new BodyDef();
        def.type = BodyDef.BodyType.DynamicBody;

        def.position.set(pos.x, pos.y);
        pBody1 = world.createBody(def);
        pBody2 = world.createBody(def);
        pBody3 = world.createBody(def);
        PolygonShape shape = new PolygonShape();
        shape.set(coords);
        pBody1.createFixture(shape, 1f);
        coords = new float[] {
                2.75f, 3.55f, 3.65f, 1.6f, 5.8f, 3.35f, 5.4f, 5.45f
        };
        shape.set(coords);
        pBody2.createFixture(shape, 1f);

        coords = new float[] {
                .15f, 4.4f, 2.75f, 3.55f, 5.4f, 5.45f
        };
        shape.set(coords);
        pBody3.createFixture(shape, 1f);

        WeldJointDef jdef = new WeldJointDef();
        jdef.bodyA = pBody1;
        jdef.bodyB = pBody2;
        jdef.collideConnected = false;
        jdef.localAnchorA.set(0.0f, 0); //???? figure this out it somehow works tho lmao
        jdef.localAnchorB.set(0.0f, 0);
        WeldJoint tmp = (WeldJoint) world.createJoint(jdef);
        joints.add(tmp);


        jdef.bodyA = pBody1;
        jdef.bodyB = pBody3;
        tmp = (WeldJoint) world.createJoint(jdef);
        joints.add(tmp);


        jdef.bodyA = pBody2;
        jdef.bodyB = pBody3;
        tmp = (WeldJoint) world.createJoint(jdef);
        joints.add(tmp);

        pBody1.setUserData(this);
        pBody2.setUserData(this);
        pBody3.setUserData(this);






    }
    public void generateRand() {
        float[] vertices = randConvex(0.2f * 10, -1f, 14, 2f, false);
        Array<Body> bA = triangulate(vertices); //creates bodies as well.


        //thread the bodies together
        WeldJointDef jdef = new WeldJointDef();
        for (int i = 1; i < bA.size; i++) {


            jdef.bodyA = bA.get(i - 1);
            jdef.bodyB = bA.get(i);
            jdef.collideConnected = false;
            jdef.localAnchorA.set(0.0f, 0); //???? figure this out it somehow works tho lmao
            jdef.localAnchorB.set(0.0f, 0);
            WeldJoint tmp = (WeldJoint) world.createJoint(jdef);
            joints.add(tmp);



          /*  Array<Joint> jointscontoBodyA = jointmap.get(jdef.bodyA);
            Array<Joint> jointscontoBodyB = jointmap.get(jdef.bodyB);
            if (jointscontoBodyA == null) {
                Array<Joint> js = new Array<Joint>();
                js.add(tmp);
                jointmap.put(jdef.bodyA, js);
            } else if (jointscontoBodyA != null) {
                jointscontoBodyA.add(tmp);
            }


            if (jointscontoBodyB == null) {
                Array<Joint> js = new Array<Joint>();
                js.add(tmp);
                jointmap.put(jdef.bodyB, js);
            } else if (jointscontoBodyB != null) {
                jointscontoBodyA.add(tmp);
            }
            */

        }


        // z = (WeldJoint)world.createJoint(jdef);

    }

    public Array<Body> triangulate(float[] vertices) {
        Array<Body> tmp = new Array<Body>();
        ShortArray doop = zrt.computeTriangles(vertices);
        //System.out.println(doop);
        doop.reverse();
        PolygonShape nut = new PolygonShape();
        float[] goober = new float[6];
        /*
        computetriangles returns an array of indices, where each index represents a vertex in the array you passed in originally.
        So if you passed in an array of size 8, that would represent 4 vertices; the array returned would be size 6
        (3 vertices for triangle 1, 3 vertices for triangle 2).
         */

        for (int i = 0; i < doop.size; i += 3) {
            goober[0] = vertices[doop.get(i) * 2];
            goober[1] = vertices[(doop.get(i) * 2) + 1];
            goober[2] = vertices[doop.get(i + 1) * 2];
            goober[3] = vertices[(doop.get(i + 1) * 2) + 1];
            goober[4] = vertices[doop.get(i + 2) * 2];
            goober[5] = vertices[(doop.get(i + 2) * 2) + 1];

            nut.set(goober);
            tmp.add(createFromShape(nut));
            System.out.println(Arrays.toString(goober));

        }


        nut.dispose();
        return tmp;
    }

    public float[] randConvex(float r, float a, int num, float scale, boolean neat) {
        Array<Float> tst = new Array<Float>();
        float x0 = 0, y0 = 0;
        float angle = 0;
        int i = 0;
        for (i = 0; i < num; i++) {
            angle += 0.5f + Math.random() * 0.3;
            if (angle > 6.2831855f) {
                break;
            }
            if (!neat) {
                r = 0.1f + (float)Math.random() * scale;
            }


            tst.add(x0 + r * MathUtils.cos(angle));
            tst.add(y0 + r * MathUtils.sin(angle));

        }
        float[] tmp = new float[tst.size];
        for (i = 0; i < tst.size; i++) {
            tmp[i] = tst.get(i);
        }

        return tmp;
    }

    public float[] randConvexCircle(float r, float a, int num, float scale, boolean neat) {
        Array<Float> tst = new Array<Float>();
        float x0 = 0, y0 = 0;
        float angle = 0;
        int i = 0;
        int safe = 1;
        for (i = 0; i < num; i++) {
            angle += 0.5f + Math.random() * 0.3;
            if (angle > 6.2831855f) {
                break;
            }
            if (!neat) {
                r = 0.1f + (float)Math.random() * scale;
            }

            float x = x0 + r * MathUtils.cos(angle);
            float y = y0 + r * MathUtils.sin(angle);
            tst.add(x);
            tst.add(y);
            safe ++;
            if (safe == 3) {
                tst.add(x0);
                tst.add(y0);
                tst.add(x);
                tst.add(y);
                safe = 2;
            }

        }
        tst.add(tst.get(0));
        tst.add(tst.get(1));
        tst.add(x0);
        tst.add(y0);

        float[] tmp = new float[tst.size];
        for (i = 0; i < tst.size; i++) {
            tmp[i] = tst.get(i);
        }

        return tmp;
    }

}

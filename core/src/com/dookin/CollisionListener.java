package com.dookin;

import com.badlogic.gdx.physics.box2d.Body;
import com.badlogic.gdx.physics.box2d.Contact;
import com.badlogic.gdx.physics.box2d.ContactImpulse;
import com.badlogic.gdx.physics.box2d.ContactListener;
import com.badlogic.gdx.physics.box2d.Fixture;
import com.badlogic.gdx.physics.box2d.Manifold;
import com.badlogic.gdx.physics.box2d.World;

/**
 * Created by David on 3/19/2018.
 */

public class CollisionListener implements ContactListener {
    World world;

    public CollisionListener(World world) {
        this.world = world;
    }

    @Override
    public void beginContact(Contact contact) {
        Fixture fa = contact.getFixtureA();
        Fixture fb = contact.getFixtureB();
        Body ba = fa.getBody();
        Body bb = fb.getBody();
        if (fa == null || fb == null) {System.out.println("what"); return;}
        //if (fa.getBody().getUserData() == null || fb.getBody().getUserData() == null) {return;}


//in order to avoid an asteroid breaking up from collisions with its own non-threaded parts,we need to use collision filters?
        if (ba.getUserData() instanceof Asteroid && bb.getUserData() instanceof Asteroid) {
            if (((Asteroid) ba.getUserData()) != ((Asteroid) bb.getUserData() )) {
                //if the 2 collided bodies are not part of the same asteroid:
                //thing is, a broken off piece of the original asteroid should be able to break the original
                ((Asteroid) ba.getUserData()).separateChunk(ba);
                ((Asteroid) bb.getUserData()).separateChunk(bb);
            }
        }//if only 1 bod is asteroid
        else if (ba.getUserData() instanceof Asteroid) {
            //((Asteroid) ba.getUserData()).separate();
           // System.out.println("shit collidedp1");
            ((Asteroid) ba.getUserData()).separateChunk(ba);
        }
        else if (bb.getUserData() instanceof Asteroid) {
            //((Asteroid) bb.getUserData()).separate();
            //System.out.println("shit collidedp2");
            ((Asteroid) bb.getUserData()).separateChunk(bb);
        }




    }

    @Override
    public void endContact(Contact contact) {

    }

    @Override
    public void preSolve(Contact contact, Manifold oldManifold) {

    }

    @Override
    public void postSolve(Contact contact, ContactImpulse impulse) {

    }
}

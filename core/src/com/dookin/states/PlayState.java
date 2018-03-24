package com.dookin.states;

import com.badlogic.gdx.Application;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Sprite;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.physics.box2d.Body;
import com.badlogic.gdx.physics.box2d.BodyDef;
import com.badlogic.gdx.physics.box2d.Box2DDebugRenderer;
import com.badlogic.gdx.physics.box2d.CircleShape;
import com.badlogic.gdx.physics.box2d.Fixture;
import com.badlogic.gdx.physics.box2d.Joint;
import com.badlogic.gdx.physics.box2d.PolygonShape;
import com.badlogic.gdx.physics.box2d.QueryCallback;
import com.badlogic.gdx.physics.box2d.World;
import com.badlogic.gdx.physics.box2d.joints.MouseJoint;
import com.badlogic.gdx.physics.box2d.joints.MouseJointDef;
import com.badlogic.gdx.utils.Array;
import com.dookin.Asteroid;
import com.dookin.CollisionListener;
import com.dookin.GestureListener;
import com.dookin.Utils;
import com.dookin.managers.GameStateManager;

import box2dLight.PointLight;
import box2dLight.RayHandler;


public class PlayState extends GameState{


    SpriteBatch batch;
    Texture img;
     private OrthographicCamera camera; //2d camera
    //private GameStateManager gsm;
    private Box2DDebugRenderer b2dr;
    private World world; //box2d world
    private Body player; //box2d body
    private Vector2 gravity;
    private Body platform;
    private RayHandler rayHandler;
    private PointLight myLight;

    private MouseJointDef mousejd;
    private MouseJoint mousej;
    public static Array<Joint> destroyedJoints = new Array<Joint>();
    public Asteroid testAsteroid;
    private float srcX = 0;
    private Sprite playerSprite;
    private Sprite planetSprite;
    private Body planet;
    private Matrix4 cmAdjusted = new Matrix4();
    private PointLight planetLight;

    public PlayState(GameStateManager gsm) {
        super(gsm);
        create();





    }

    private void create() {

        batch = new SpriteBatch();
        img = new Texture("bg3.jpg");
        playerSprite = new Sprite(new Texture("ship.png"));
        planetSprite = new Sprite(new Texture("planet.png"));
        img.setWrap(Texture.TextureWrap.Repeat, Texture.TextureWrap.Repeat);
        camera = new OrthographicCamera();
        camera.setToOrtho(false,Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
		/*  box2d setup */
        gravity = new Vector2(0,0);
        world = new World(gravity, false);
        b2dr = new Box2DDebugRenderer();
        player = createPlayer(-4,-4);
        //platform  = createPlatform();
        planet = createPlanet();
        rayHandler = new RayHandler(world);
        rayHandler.setAmbientLight(.4f);
        myLight = new PointLight(rayHandler, 100, Color.CYAN, 300/Utils.PPM, 0, 0);
        planetLight = new PointLight(rayHandler, 100, Color.GRAY, ((CircleShape)planet.getFixtureList().get(0).getShape()).getRadius() * 1.5f, planet.getPosition().x, planet.getPosition().y);
        planetLight.setXray(true);
        //planetLight.setSoft(true);
        //planetLight.setSoftnessLength(90.0f);
        myLight.setSoftnessLength(0.0f);
        //myLight.setXray(true);
        //rayHandler.setCombinedMatrix(camera.combined.scl(Utils.PPM));
        myLight.attachToBody(player);
        //b2dr.setDrawBodies(false);
        cameraUpdate();
        //since camera always follows player, we're drawing the thing on top of the player since player is always in the center of camera
        testAsteroid = new Asteroid(stob2d(new Vector3(20, 20, 0)), world, 1,5, null);

        createPlayer(-20,-20);


        //mousejoint
        mousejd = new MouseJointDef();
        mousejd.bodyA = world.createBody(new BodyDef()); //not actually used
        mousejd.bodyB = player; //one that is actually moving around
        mousejd.collideConnected = true;
        mousejd.maxForce = 100;
        //SETUP PROCESSORS AND LISTENERS//
        world.setContactListener(new CollisionListener(world));
        Gdx.input.setInputProcessor(new GestureListener(world, mousej, mousejd, camera, player));


    }

    @Override
    public void resize(int w, int h) {
        if (Gdx.app.getType().equals(Application.ApplicationType.Android)) {
            System.out.println(Gdx.graphics.getWidth() + " " + Gdx.graphics.getHeight());
            camera.setToOrtho(false,Gdx.graphics.getWidth()/2, Gdx.graphics.getHeight()/2);
            return;
        }
        System.out.println(Gdx.graphics.getWidth() + " " + Gdx.graphics.getHeight());
        camera.setToOrtho(false,Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
    }

    @Override
    public void update(float delta) {



//each planet should have an update method with a query AABB for its shit
        float x = ((CircleShape)planet.getFixtureList().get(0).getShape()).getRadius();


        world.QueryAABB(nut, planet.getPosition().x-(x * 3), planet.getPosition().x-(x * 3), planet.getPosition().x+(x * 3), planet.getPosition().x+(x * 3));








        world.step(1/60f, 6, 2);
        cameraUpdate(); //camera's orthographic x and y world coords in pixels are set to player's coords in pixels
        //camera.combined.scl(1f/Utils.PPM);

        //RAYHANDLER UPDATE GARBAGE: literally magic, i don't know why scaling works
        //camera.combined.scl(Utils.PPM); // i don't know why we scale PPM instead of 1/PPM
        //rayHandler.setCombinedMatrix(camera.combined.cpy().scl(Utils.PPM));
        //rayHandler.setCombinedMatrix(camera);
        //rayHandler.update();
        //camera.combined.scl(1f/Utils.PPM);
        //cameraUpdate();

        objectCleanup();
        //--------------------------

        //combined matrix contains the view matrix(where shit is in your 3d world)
        //as well as the projection matrix(how do we map the shit in the 3d world to a 2d plane(the camera)
    }

    @Override
    public void render() {
        Gdx.gl.glClearColor(0, 0, 0, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);


        //update(Gdx.graphics.getDeltaTime()); //UPDATE BY DEFAULT IS CALLED BY THE APPLICATION BEFORE RENDER NO MATTER WHAT

        batch.setProjectionMatrix(camera.combined); //
        batch.begin();

        batch.draw(img, -Gdx.graphics.getWidth() / 2, -Gdx.graphics.getHeight()/2, (int)srcX, 0, img.getWidth() * 5, img.getHeight() * 5);

        playerSprite.setScale(.5f);
        playerSprite.setCenter(Utils.m2p(player.getPosition().x), Utils.m2p(player.getPosition().y));
        playerSprite.setRotation(player.getAngle()* MathUtils.radDeg - 90);
        planetSprite.setCenter(Utils.m2p(planet.getPosition().x), Utils.m2p(planet.getPosition().y));
        planetSprite.setSize(Utils.m2p(((CircleShape)planet.getFixtureList().get(0).getShape()).getRadius() * 2), Utils.m2p(((CircleShape)planet.getFixtureList().get(0).getShape()).getRadius() * 2));
        planetSprite.draw(batch);
        playerSprite.draw(batch);


        batch.end();

        b2dr.render(world, camera.combined.scl(Utils.PPM)); //scaled by ppm since camera is in pixels and 1 meter is 32 pixels so meters -> pixels *=32 //camera.combined matrix is scaled by ppm




        rayHandler.setCombinedMatrix(camera.combined); //camera combined was already scaled. //not really deprecated since calling setCombinedMatrix(camera) is essentially the same but for some reason breaks
        rayHandler.updateAndRender();

        updateInput(); //unfortunately because of precedence

    }

    @Override
    public void dispose() {
        rayHandler.dispose();
        batch.dispose();
        img.dispose();
        world.dispose();
        b2dr.dispose();
    }

    private void objectCleanup() {
        for(Joint a: destroyedJoints) {
            world.destroyJoint(a);
            a = null;
        }
        destroyedJoints.clear();


    }
    private void updateInput() {
        /*this input is only for changing states or gamewide info*/
        if (Gdx.input.isKeyJustPressed(Input.Keys.R)) {
            gsm.setState(GameStateManager.state.GAME);
        }
    }

    public Body createPlayer(float x, float y) {
        Body pBody;

        //STEP 1, BODY DEFINITION: position, friction, misc properties
        BodyDef def = new BodyDef(); //body definition, describes physical properties body will have.
        //friction, type of body, etc
        def.type = BodyDef.BodyType.DynamicBody;
        def.angle = 90 * MathUtils.degRad;
        def.position.set(x,y); //b2d world coords
        //def.fixedRotation = true; //no rotations
        pBody = world.createBody(def);//actually create body.
        //STEP 2, CREATE SHAPE
        PolygonShape shape = new PolygonShape();
        //box 2d works in meters. pixels per meter is important
        //shape.set(new float[]{0,0,1,0,0.5f,1});
        shape.setAsBox(Utils.p2m(32.0f/2.0f), Utils.p2m(32.0f/2.0f)); //box2d takes height and width from center. so actual width is 16 * 2 = 32
        //STEP 3 ASSIGN FIXTURE TO BODY
        pBody.createFixture(shape, 3.0f); //the fixture is attached to the shape and gives it a density


        //cleanup
        shape.dispose();

        return pBody;
    }

    private Vector3 stob2d(Vector3 s) {
        //System.out.println(s);
        //camera.update();
        camera.unproject(s);
        //System.out.println(s);
        return s.set(Utils.p2m(s.x), Utils.p2m(s.y), 0);
    }

    private Body createPlanet() {
        Body pBody;


        BodyDef def = new BodyDef();
        def.type = BodyDef.BodyType.StaticBody;
        //def.angle = 90 * MathUtils.degRad;
        def.position.set(0,0);
        pBody = world.createBody(def);

        //PolygonShape shape = new PolygonShape();
        CircleShape shape = new CircleShape();
        shape.setRadius(10f);
        pBody.createFixture(shape, 3.0f);
        pBody.getFixtureList().get(0).setFriction(0.8f);


        //cleanup
        shape.dispose();
        return pBody;

    }
    public void cameraUpdate() {
        Vector3 position = camera.position;
        position.x = player.getPosition().x * Utils.PPM;
        position.y = player.getPosition().y * Utils.PPM;
        camera.position.set(position);
        camera.update();
    }
    public Body createPlatform() {
        return null;
    }

    private QueryCallback nut = new QueryCallback() {
        @Override
        public boolean reportFixture(Fixture fixture) {
            if (fixture.getBody() == planet) {
                return false;
            }
            Vector2 plan2Deb = new Vector2();
            plan2Deb.set(fixture.getBody().getPosition().x - planet.getPosition().x, fixture.getBody().getPosition().y-planet.getPosition().y);
            plan2Deb.scl(-1f);

            float rad = ((CircleShape)planet.getFixtureList().get(0).getShape()).getRadius();
            float dist = Math.abs(plan2Deb.x) + Math.abs(plan2Deb.y);

            plan2Deb.scl((1f/dist)*rad/plan2Deb.len() * 34);
            Float flt = new Float(plan2Deb.x);
            if (flt.isNaN()) {
                System.out.println(fixture.getBody() == planet);
            }
            //System.out.println(plan2Deb); //static bodies do not have mass : /

            fixture.getBody().applyForceToCenter(plan2Deb, true);

//System.out.println("what");
            return true;
        }
    };


}

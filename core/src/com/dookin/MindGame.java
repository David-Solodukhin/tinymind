package com.dookin;

import com.badlogic.gdx.Application;
import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
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
import com.badlogic.gdx.physics.box2d.Joint;
import com.badlogic.gdx.physics.box2d.PolygonShape;
import com.badlogic.gdx.physics.box2d.World;
import com.badlogic.gdx.physics.box2d.joints.MouseJoint;
import com.badlogic.gdx.physics.box2d.joints.MouseJointDef;
import com.badlogic.gdx.utils.Array;

import box2dLight.PointLight;
import box2dLight.RayHandler;

public class MindGame extends ApplicationAdapter {
	SpriteBatch batch;
	Texture img;
	private OrthographicCamera camera; //2d camera

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
	private Matrix4 cmAdjusted = new Matrix4();

	@Override
	public void create () {
		batch = new SpriteBatch();
		img = new Texture("bg3.jpg");
		playerSprite = new Sprite(new Texture("ship.png"));
		img.setWrap(Texture.TextureWrap.Repeat, Texture.TextureWrap.Repeat);
		camera = new OrthographicCamera();
		camera.setToOrtho(false,Gdx.graphics.getWidth(), Gdx.graphics.getHeight());

		/*  box2d setup */
		gravity = new Vector2(0,0);
		world = new World(gravity, false);
		b2dr = new Box2DDebugRenderer();
		player = createPlayer(0,0);
		//platform  = createPlatform();

		rayHandler = new RayHandler(world);
		rayHandler.setAmbientLight(.4f);
		myLight = new PointLight(rayHandler, 100, Color.CYAN, 300/Utils.PPM, 0, 0);
		myLight.setSoftnessLength(0.0f);
		//myLight.setXray(true);
		//rayHandler.setCombinedMatrix(camera.combined.scl(Utils.PPM));
		myLight.attachToBody(player);

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




		//myLight.attachToBody(player);
	}

	@Override
	public void render () {
		 //amount of time between frame refresh
		Gdx.gl.glClearColor(0, 0, 0, 1);
		Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);


		/*
		set misc variables needed for rendering things here:
		 */
		//srcX++;
		/*
		1. update physics world(world.step etc)


		2. update camera //points camera in correct direction
		2.5 draw any backgrounds that go behind ALL the physics stuff
		3. box2d debug render //draws base physics stuff
		3.5 any other stuff you want light to be affected by drawn, all spritebatches etc
		4. rayhandler update and render
		5.
		 */
		update(Gdx.graphics.getDeltaTime());

		batch.setProjectionMatrix(camera.combined); //
		batch.begin();

		batch.draw(img, -Gdx.graphics.getWidth() / 2, -Gdx.graphics.getHeight()/2, (int)srcX, 0, img.getWidth() * 5, img.getHeight() * 5);

		playerSprite.setScale(.5f);
		playerSprite.setCenter(Utils.m2p(player.getPosition().x), Utils.m2p(player.getPosition().y));
		playerSprite.setRotation(player.getAngle()* MathUtils.radDeg - 90);
		playerSprite.draw(batch);


		batch.end();

		b2dr.render(world, camera.combined.scl(Utils.PPM)); //scaled by ppm since camera is in pixels and 1 meter is 32 pixels so meters -> pixels *=32 //camera.combined matrix is scaled by ppm




		rayHandler.setCombinedMatrix(camera.combined); //camera combined was already scaled. //not really deprecated since calling setCombinedMatrix(camera) is essentially the same but for some reason breaks
		rayHandler.updateAndRender();




	}

	@Override
	public void resize(int width, int height) {
		if (Gdx.app.getType().equals(Application.ApplicationType.Android)) {
			System.out.println(Gdx.graphics.getWidth() + " " + Gdx.graphics.getHeight());
			camera.setToOrtho(false,Gdx.graphics.getWidth()/2, Gdx.graphics.getHeight()/2);
			return;
		}
		System.out.println(Gdx.graphics.getWidth() + " " + Gdx.graphics.getHeight());
		camera.setToOrtho(false,Gdx.graphics.getWidth(), Gdx.graphics.getHeight()); //????
	}
	@Override
	public void dispose () {
		rayHandler.dispose();
		batch.dispose();
		img.dispose();
		world.dispose();
		b2dr.dispose();
	}
	public void update(float delta) {
		updateInput();
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

	private void objectCleanup() {
		for(Joint a: destroyedJoints) {
			world.destroyJoint(a);
			a = null;
		}
		destroyedJoints.clear();


	}


	private void updateInput() {

		if(Gdx.input.isTouched()) {


			//System.out.println(" screen coords: " + Gdx.input.getX() + " " + Gdx.input.getY());
			 //alt way to check if point in body

			/*for(Fixture fixture : player.getFixtureList()) {
				if(fixture.testPoint(Utils.p2m(currentTouched.x), Utils.p2m(currentTouched.y))) {
					System.out.println("touched"); //the log isn't accurate for this shit since too many msgs will just be ignored by log
					player.setTransform(new Vector2(Utils.p2m(currentTouched.x), Utils.p2m(currentTouched.y)),0 );





				}
			}*/

		}



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


}

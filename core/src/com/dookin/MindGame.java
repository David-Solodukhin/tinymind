package com.dookin;

import com.badlogic.gdx.Application;
import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.InputProcessor;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.physics.box2d.Body;
import com.badlogic.gdx.physics.box2d.BodyDef;
import com.badlogic.gdx.physics.box2d.Box2DDebugRenderer;
import com.badlogic.gdx.physics.box2d.Fixture;
import com.badlogic.gdx.physics.box2d.Joint;
import com.badlogic.gdx.physics.box2d.PolygonShape;
import com.badlogic.gdx.physics.box2d.QueryCallback;
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

	private Vector3 currentTouched;
	private Vector3 tmp = new Vector3();
	private Vector2 tmp2 = new Vector2();
	private MouseJointDef mousejd;
	private MouseJoint mousej;
	public static Array<Joint> destroyedJoints = new Array<Joint>();
	public Asteroid testAsteroid;
	@Override
	public void create () {
		System.out.println(Gdx.graphics.getWidth() + " " + Gdx.graphics.getHeight());
		batch = new SpriteBatch();
		//img = new Texture("badlogic.jpg");
		camera = new OrthographicCamera();
		camera.setToOrtho(false,Gdx.graphics.getWidth(), Gdx.graphics.getHeight());

		/*  box2d setup */
		gravity = new Vector2(0,0);
		world = new World(gravity, false);
		b2dr = new Box2DDebugRenderer();
		player = createPlayer(0,0);
		//platform  = createPlatform();

		rayHandler = new RayHandler(world);
		rayHandler.setAmbientLight(.5f);
		myLight = new PointLight(rayHandler, 400, Color.WHITE, 300/Utils.PPM, 40.0f/32.0f, 40.0f/32.0f);
		myLight.setSoftnessLength(0.0f);
		//rayHandler.setCombinedMatrix(camera.combined.scl(Utils.PPM));
		mousejd = new MouseJointDef();

		mousejd.bodyA = world.createBody(new BodyDef()); //not actually used
		mousejd.bodyB = player; //one that is actually moving around
		mousejd.collideConnected = true;
		mousejd.maxForce = 100;
		cameraUpdate();
		//since camera always follows player, we're drawing the thing on top of the player since player is always in the center of camera
		testAsteroid = new Asteroid(stob2d(new Vector3(20, 20, 0)), world, 2,5, null);

		createPlayer(-6,-6);

		world.setContactListener(new CollisionListener(world));
		Gdx.input.setInputProcessor(new InputProcessor() {
			@Override
			public boolean keyDown(int keycode) {
				return false;
			}

			@Override
			public boolean keyUp(int keycode) {
				return false;
			}

			@Override
			public boolean keyTyped(char character) {
				return false;
			}

			@Override
			public boolean touchDown(int screenX, int screenY, int pointer, int button) {
				camera.unproject(tmp.set(screenX, screenY, 0));
				//System.out.println("world space coords: " + test.x/32.0f + " " + test.y/32.0f);
				//tmp contains world coords and so does currentouched
				tmp.set(Utils.p2m(tmp.x), Utils.p2m(tmp.y), 0);
				world.QueryAABB(queryCallback, tmp.x, tmp.y, tmp.x, tmp.y);

				return true;
			}

			@Override
			public boolean touchUp(int screenX, int screenY, int pointer, int button) {
				if (mousej == null) {
					return false;
				}
				world.destroyJoint(mousej);
				mousej = null;
				return true;
			}

			@Override
			public boolean touchDragged(int screenX, int screenY, int pointer) {
				if (mousej == null) {
					return false;
				}
				camera.unproject(tmp.set(screenX, screenY, 0));
				tmp.set(Utils.p2m(tmp.x), Utils.p2m(tmp.y),0);
				tmp2.set(tmp.x, tmp.y);
				mousej.setTarget(tmp2);
				//System.out.println("move to here drag: "+ Utils.p2m(currentTouched.x)+" "+ Utils.p2m(currentTouched.y));
				return true;
			}

			@Override
			public boolean mouseMoved(int screenX, int screenY) {
				return false;
			}

			@Override
			public boolean scrolled(int amount) {
				return false;
			}
		});




		//myLight.attachToBody(player);
	}

	@Override
	public void render () {
		update(Gdx.graphics.getDeltaTime()); //amount of time between frame refresh
		Gdx.gl.glClearColor(0, 0, 0, 1);
		Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

		b2dr.render(world, camera.combined.scl(Utils.PPM));
		rayHandler.render();
		//System.out.println(Gdx.graphics.getFramesPerSecond());
		/*
		batch.begin();
		batch.draw(img, 0, 0);
		batch.end();
		*/
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
		//img.dispose();
		world.dispose();
		b2dr.dispose();
	}
	public void update(float delta) {
		updateInput();
		world.step(1/60f, 6, 2);


		cameraUpdate();
		//camera.combined.scl(1f/Utils.PPM);

		//RAYHANDLER UPDATE GARBAGE: literally magic, i don't know why scaling works
		camera.combined.scl(Utils.PPM); // i don't know why we scale PPM instead of 1/PPM
		rayHandler.setCombinedMatrix(camera);
		rayHandler.update();
		camera.combined.scl(1f/Utils.PPM);


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

	private QueryCallback queryCallback = new QueryCallback() {
		@Override
		public boolean reportFixture(Fixture fixture) {
			camera.unproject(tmp.set(Gdx.input.getX(), Gdx.input.getY(), 0));
			tmp.set(Utils.p2m(tmp.x), Utils.p2m(tmp.y), 0);



			if (!fixture.testPoint(tmp.x, tmp.y)) {
				return true; //if this is not the fixture we touched, get the next one
			}
			mousejd.bodyB = fixture.getBody();

			mousejd.target.set(tmp.x, tmp.y);
			mousej = (MouseJoint)world.createJoint(mousejd);

			return true;
		}
	};
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

		def.position.set(x,y); //b2d world coords
		//def.fixedRotation = true; //no rotations
		pBody = world.createBody(def);//actually create body.
		//STEP 2, CREATE SHAPE
		PolygonShape shape = new PolygonShape();
		//box 2d works in meters. pixels per meter is important
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

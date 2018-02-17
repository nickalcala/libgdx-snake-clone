package com.niceprogrammer.snake.screen;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.ScreenAdapter;
import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer.ShapeType;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.badlogic.gdx.utils.viewport.Viewport;

public class GameScreen extends ScreenAdapter {

	private static final float WORLD_WIDTH = 640, WORLD_HEIGHT = 480;
	private static final int RIGHT = 0, LEFT = 1, UP = 2, DOWN = 3;
	private static final int GRID_CELL = 32;
	private static final String GAME_OVER_TEXT = "Game Over. Tap [SPACE] to restart!";
	private static final float MOVE_TIME = 0.2f;
	private static final int SNAKE_MOVEMENT = 32;
	private static final int POINTS_PER_APPLE = 20;

	// Game
	private enum STATE {
		PLAYING, GAME_OVER, PAUSED
	}

	private STATE state = STATE.PLAYING;
	private SpriteBatch batch;
	private ShapeRenderer shapeRenderer;
	private boolean directionSet = false;
	private BitmapFont bitmapFont;
	private GlyphLayout glyphLayout = new GlyphLayout();
	private Viewport viewport;
	private Camera camera;
	private int score = 0;

	// Snake
	private Texture snakeHead;
	private Texture snakeBody;
	private float snakeX = 0, snakeY = 0;
	private float snakeXBeforeUpdate = 0, snakeYBeforeUpdate = 0;
	private float timer = MOVE_TIME;
	private int snakeDirection = 0;
	private Array<BodyPart> bodyParts = new Array<BodyPart>();

	// Apple
	private Texture apple;
	private boolean isAppleAvailable = false;
	private int appleX, appleY;

	@Override
	public void show() {
		camera = new OrthographicCamera(Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
		camera.position.set(WORLD_WIDTH / 2, WORLD_HEIGHT / 2, 0);
		camera.update();
		viewport = new FitViewport(WORLD_WIDTH, WORLD_HEIGHT, camera);
		batch = new SpriteBatch();
		shapeRenderer = new ShapeRenderer();
		snakeHead = new Texture(Gdx.files.internal("snakeHead.png"));
		snakeBody = new Texture(Gdx.files.internal("snakeBody.png"));
		apple = new Texture(Gdx.files.internal("apple.png"));
		bitmapFont = new BitmapFont();
	}

	@Override
	public void render(float delta) {

		boolean pPressed = Gdx.input.isKeyPressed(Input.Keys.P);
		if (pPressed) {
			if (state == STATE.PAUSED) {
				state = STATE.PLAYING;
			} else {
				state = STATE.PAUSED;
			}
		}

		switch (state) {
		case PLAYING: {
			queryInput();
			updateSnake(delta);
			checkAppleCollision();
			checkAndPlaceApple();
		}
			break;
		case GAME_OVER: {
			checkForRestart();
		}
			break;
		case PAUSED: {

		}
			break;
		}
		clearScreen();
		drawGrid();
		draw();
	}

	private void updateSnake(float delta) {
		timer -= delta;
		if (timer <= 0) {
			timer = MOVE_TIME;
			moveSnake();
			checkForOutOfBounds();
			updateBodyPartsPosition();
			checkSnakeBodyCollision();
			directionSet = false;
		}
	}

	private void clearScreen() {
		Gdx.gl.glClearColor(Color.BLACK.r, Color.BLACK.g, Color.BLACK.b, Color.BLACK.a);
		Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
	}

	private void draw() {
		batch.setProjectionMatrix(camera.projection);
		batch.setTransformMatrix(camera.view);
		batch.begin();
		batch.draw(snakeHead, snakeX, snakeY);
		for (BodyPart bodyPart : bodyParts) {
			bodyPart.draw(batch);
		}
		if (isAppleAvailable) {
			batch.draw(apple, appleX, appleY);
		}
		if (state == STATE.GAME_OVER) {
			glyphLayout.setText(bitmapFont, GAME_OVER_TEXT);
			bitmapFont.draw(batch, GAME_OVER_TEXT, (viewport.getWorldWidth() - glyphLayout.width) / 2,
					(viewport.getWorldHeight() - glyphLayout.height) / 2);
		}
		drawScore();
		batch.end();
	}

	private void drawGrid() {
		shapeRenderer.setProjectionMatrix(camera.projection);
		shapeRenderer.setTransformMatrix(camera.view);
		shapeRenderer.begin(ShapeType.Line);
		for (int x = 0; x < viewport.getWorldWidth(); x += GRID_CELL) {
			for (int y = 0; y <= viewport.getWorldHeight(); y += GRID_CELL) {
				shapeRenderer.rect(x, y, GRID_CELL, GRID_CELL);
			}
		}
		shapeRenderer.end();
	}
	
	private void drawScore() {
		if (state == STATE.PLAYING) {
			String scoreAsString = Integer.toString(score);
			glyphLayout.setText(bitmapFont, scoreAsString);
			bitmapFont.draw(batch, scoreAsString, viewport.getWorldWidth() - glyphLayout.width,
					viewport.getWorldHeight());
		}
	}

	private void checkForOutOfBounds() {
		if (snakeX >= viewport.getWorldWidth()) {
			snakeX = 0;
		}
		if (snakeX < 0) {
			snakeX = viewport.getWorldWidth();
		}
		if (snakeY >= viewport.getWorldHeight()) {
			snakeY = 0;
		}
		if (snakeY < 0) {
			snakeY = viewport.getWorldHeight();
		}
	}

	private void moveSnake() {
		snakeXBeforeUpdate = snakeX;
		snakeYBeforeUpdate = snakeY;

		switch (snakeDirection) {
		case RIGHT: {
			snakeX += SNAKE_MOVEMENT;
		}
			break;
		case LEFT: {
			snakeX -= SNAKE_MOVEMENT;
		}
			break;
		case UP: {
			snakeY += SNAKE_MOVEMENT;
		}
			break;
		case DOWN: {
			snakeY -= SNAKE_MOVEMENT;
		}
			break;
		}
	}

	private void queryInput() {
		boolean wPressed = Gdx.input.isKeyPressed(Input.Keys.W);
		boolean sPressed = Gdx.input.isKeyPressed(Input.Keys.S);
		boolean aPressed = Gdx.input.isKeyPressed(Input.Keys.A);
		boolean dPressed = Gdx.input.isKeyPressed(Input.Keys.D);
		if (wPressed) {
			updateDirection(UP);
		}
		if (sPressed) {
			updateDirection(DOWN);
		}
		if (aPressed) {
			updateDirection(LEFT);
		}
		if (dPressed) {
			updateDirection(RIGHT);
		}
	}

	private void checkForRestart() {
		if (Gdx.input.isKeyPressed(Input.Keys.SPACE)) {
			doRestart();
		}
	}

	private void doRestart() {
		state = STATE.PLAYING;
		bodyParts.clear();
		snakeDirection = RIGHT;
		directionSet = false;
		timer = MOVE_TIME;
		snakeX = 0;
		snakeY = 0;
		snakeXBeforeUpdate = 0;
		snakeYBeforeUpdate = 0;
		isAppleAvailable = false;
		score = 0;
	}

	private void updateDirection(int newSnakeDirection) {
		if (!directionSet && snakeDirection != newSnakeDirection) {
			directionSet = true;
			switch (newSnakeDirection) {
			case LEFT: {
				updateIfNotOppositeDirection(newSnakeDirection, RIGHT);
			}
				break;
			case RIGHT: {
				updateIfNotOppositeDirection(newSnakeDirection, LEFT);
			}
				break;
			case UP: {
				updateIfNotOppositeDirection(newSnakeDirection, DOWN);
			}
				break;
			case DOWN: {
				updateIfNotOppositeDirection(newSnakeDirection, UP);
			}
				break;
			}
		}
	}

	private void checkAndPlaceApple() {
		if (!isAppleAvailable) {
			do {
				appleX = MathUtils.random((int) (viewport.getWorldWidth() / SNAKE_MOVEMENT - 1)) * SNAKE_MOVEMENT;
				appleY = MathUtils.random((int) (viewport.getWorldHeight() / SNAKE_MOVEMENT - 1)) * SNAKE_MOVEMENT;
				isAppleAvailable = true;
			} while (appleX == snakeX && appleY == snakeY);
		}
	}

	private void checkAppleCollision() {
		if (appleX == snakeX && appleY == snakeY && isAppleAvailable) {
			BodyPart bodyPart = new BodyPart(snakeBody);
			bodyPart.updateBodyPosition(snakeX, snakeY);
			bodyParts.insert(0, bodyPart);
			addToScore();
			isAppleAvailable = false;
		}
	}

	private void addToScore() {
		score += POINTS_PER_APPLE;
	}

	private void checkSnakeBodyCollision() {
		for (BodyPart part : bodyParts) {
			if (part.x == snakeX && part.y == snakeY) {
				state = STATE.GAME_OVER;
			}
		}
	}

	private void updateBodyPartsPosition() {
		if (bodyParts.size > 0) {
			BodyPart bodyPart = bodyParts.removeIndex(0);
			bodyPart.updateBodyPosition(snakeXBeforeUpdate, snakeYBeforeUpdate);
			bodyParts.add(bodyPart);
		}
	}

	private void updateIfNotOppositeDirection(int newSnakeDirection, int oppositeDirection) {
		if (snakeDirection != oppositeDirection || bodyParts.size == 0) {
			snakeDirection = newSnakeDirection;
		}
	}

	@Override
	public void resize(int width, int height) {
		viewport.update(width, height);
	}

	@Override
	public void dispose() {
		batch.dispose();
		snakeHead.dispose();
	}

	private class BodyPart {

		private float x, y;
		private Texture texture;

		public BodyPart(Texture texture) {
			this.texture = texture;
		}

		public void updateBodyPosition(float x, float y) {
			this.x = x;
			this.y = y;
		}

		public void draw(Batch batch) {
			if (!(x == snakeX && y == snakeY)) {
				batch.draw(texture, x, y);
			}
		}
	}
}
/**
 * Copyright 2008 - 2015 The Loon Game Engine Authors
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 * 
 * @project loon
 * @author cping
 * @email：javachenpeng@yahoo.com
 * @version 0.5
 */
package loon;

import loon.action.ActionControl;
import loon.action.camera.BaseCamera;
import loon.action.camera.EmptyCamera;
import loon.action.collision.GravityHandler;
import loon.action.sprite.ISprite;
import loon.action.sprite.Sprites;
import loon.action.sprite.Sprites.SpriteListener;
import loon.canvas.LColor;
import loon.component.Desktop;
import loon.component.LComponent;
import loon.component.LLayer;
import loon.component.layout.LayoutConstraints;
import loon.component.layout.LayoutManager;
import loon.component.layout.LayoutPort;
import loon.event.GameKey;
import loon.event.GameTouch;
import loon.event.LTouchArea;
import loon.event.LTouchLocation;
import loon.event.SysInput;
import loon.event.SysTouch;
import loon.event.Updateable;
import loon.event.LTouchArea.Event;
import loon.geom.PointI;
import loon.geom.RectBox;
import loon.geom.XY;
import loon.opengl.GLEx;
import loon.opengl.LSTRDictionary;
import loon.opengl.LTextureImage;
import loon.stage.Player;
import loon.stage.PlayerUtils;
import loon.stage.RootPlayer;
import loon.stage.Stage;
import loon.stage.StageSystem;
import loon.stage.StageTransition;
import loon.utils.TArray;
import loon.utils.processes.GameProcess;
import loon.utils.processes.RealtimeProcess;
import loon.utils.processes.RealtimeProcessManager;
import loon.utils.reply.Closeable;
import loon.utils.reply.Port;
import loon.utils.res.ResourceLocal;
import loon.utils.timer.LTimer;
import loon.utils.timer.LTimerContext;

public abstract class Screen extends PlayerUtils implements SysInput, LRelease,
		XY {

	/**
	 * 最后绘制用户界面
	 */
	public void lastUserDraw() {
		setFristOrder(DRAW_SPRITE_PAINT());
		setSecondOrder(DRAW_DESKTOP_PAINT());
		setLastOrder(DRAW_USER_PAINT());
	}

	/**
	 * 优先绘制用户界面
	 */
	public void fristUserDraw() {
		setFristOrder(DRAW_USER_PAINT());
		setSecondOrder(DRAW_SPRITE_PAINT());
		setLastOrder(DRAW_DESKTOP_PAINT());
	}

	/**
	 * 把用户渲染置于精灵与桌面之间
	 */
	public void centerUserDraw() {
		setFristOrder(DRAW_SPRITE_PAINT());
		setSecondOrder(DRAW_USER_PAINT());
		setLastOrder(DRAW_DESKTOP_PAINT());
	}

	/** 受限函数,关系到线程的同步与异步，使用此部分函数实现的功能，将无法在GWT编译的HTML5环境运行，所以默认注释掉. **/
	/**
	 * 但是，TeaVM之类的Bytecode to JS转码器是支持的.因此视情况有恢复可能性，但千万注意，恢复此部分函数的话。[不保证完整的跨平台性]
	 **/
	/*
	 * private boolean isDrawing;
	 * 
	 * @Deprecated public void yieldDraw() { notifyDraw(); waitUpdate(); }
	 * 
	 * @Deprecated public void yieldUpdate() { notifyUpdate(); waitDraw(); }
	 * 
	 * @Deprecated public synchronized void notifyDraw() { this.isDrawing =
	 * true; this.notifyAll(); }
	 * 
	 * @Deprecated public synchronized void notifyUpdate() { this.isDrawing =
	 * false; this.notifyAll(); }
	 * 
	 * @Deprecated public synchronized void waitDraw() { for (; !isDrawing;) {
	 * try { this.wait(); } catch (InterruptedException ex) { } } }
	 * 
	 * @Deprecated public synchronized void waitUpdate() { for (; isDrawing;) {
	 * try { this.wait(); } catch (InterruptedException ex) { } } }
	 * 
	 * @Deprecated public synchronized void waitFrame(int i) { for (int wait =
	 * frame + i; frame < wait;) { try { super.wait(); } catch (Exception ex) {
	 * } } }
	 * 
	 * @Deprecated public synchronized void waitTime(long i) { for (long time =
	 * System.currentTimeMillis() + i; System .currentTimeMillis() < time;) try
	 * { super.wait(time - System.currentTimeMillis()); } catch (Exception ex) {
	 * } }
	 */
	/** 受限函数结束 **/

	private LTransition _transition;

	protected final Closeable.Set _conns = new Closeable.Set();

	private LayoutConstraints _rootConstraints;

	private boolean _isExistCamera = false;

	private BaseCamera _baseCamera;

	public Screen setCamera(BaseCamera came) {
		_isExistCamera = (came != null);
		if (_isExistCamera) {
			_baseCamera = came;
			_baseCamera.setup();
		}
		return this;
	}

	public BaseCamera getCamera() {
		if (_baseCamera == null) {
			_baseCamera = new EmptyCamera();
		}
		return _baseCamera;
	}

	public LayoutConstraints getRootConstraints() {
		if (_rootConstraints == null) {
			_rootConstraints = new LayoutConstraints();
		}
		return _rootConstraints;
	}

	public LayoutPort getLayoutPort() {
		return new LayoutPort(getBox(), getRootConstraints());
	}

	public LayoutPort getLayoutPort(final RectBox newBox,
			final LayoutConstraints newBoxConstraints) {
		return new LayoutPort(newBox, newBoxConstraints);
	}

	public LayoutPort getLayoutPort(final LayoutPort src) {
		return new LayoutPort(src);
	}

	public void layoutElements(final LayoutManager manager,
			final LComponent... comps) {
		if (manager != null) {
			manager.layoutElements(this, comps);
		}
	}

	public void layoutElements(final LayoutManager manager,
			final LayoutPort... ports) {
		if (manager != null) {
			manager.layoutElements(getLayoutPort(), ports);
		}
	}

	public void stopRepaint() {
		LSystem.AUTO_REPAINT = false;
	}

	public void startRepaint() {
		LSystem.AUTO_REPAINT = true;
	}

	public void stopProcess() {
		this.processing = false;
	}

	public void startProcess() {
		this.processing = true;
	}

	private final TArray<LTouchArea> _touchAreas = new TArray<LTouchArea>();

	public void registerTouchArea(final LTouchArea touchArea) {
		this._touchAreas.add(touchArea);
	}

	public boolean unregisterTouchArea(final LTouchArea touchArea) {
		return this._touchAreas.remove(touchArea);
	}

	public void clearTouchAreas() {
		this._touchAreas.clear();
	}

	public TArray<LTouchArea> getTouchAreas() {
		return this._touchAreas;
	}

	private final void updateTouchArea(final LTouchArea.Event e,
			final float touchX, final float touchY) {
		if (this._touchAreas.size == 0) {
			return;
		}
		final TArray<LTouchArea> touchAreas = this._touchAreas;
		final int touchAreaCount = touchAreas.size;
		if (touchAreaCount > 0) {
			for (int i = 0; i < touchAreaCount; i++) {
				final LTouchArea touchArea = touchAreas.get(i);
				if (touchArea.contains(touchX, touchY)) {
					touchArea.onAreaTouched(e, touchX, touchY);
				}
			}
		}
	}

	public Screen add(Port<LTimerContext> timer) {
		if (LSystem._base != null && LSystem._base.display() != null) {
			_conns.add(LSystem._base.display().update.connect(timer));
		}
		return this;
	}

	private TArray<LRelease> releases;

	public Screen putRelease(LRelease r) {
		if (releases == null) {
			releases = new TArray<LRelease>(10);
		}
		releases.add(r);
		return this;
	}

	public Screen removeRelease(LRelease r) {
		if (releases == null) {
			releases = new TArray<LRelease>(10);
		}
		releases.remove(r);
		return this;
	}

	public Screen putReleases(LRelease... rs) {
		if (releases == null) {
			releases = new TArray<LRelease>(10);
		}
		final int size = rs.length;
		for (int i = 0; i < size; i++) {
			releases.add(rs[i]);
		}
		return this;
	}

	public float getDeltaTime() {
		return ((float) elapsedTime) / 1000f;
	}

	private RootPlayer _players;

	public final static byte DRAW_EMPTY = -1;

	public final static byte DRAW_USER = 0;

	public final static byte DRAW_SPRITE = 1;

	public final static byte DRAW_DESKTOP = 2;

	public final static byte DRAW_STAGE = 3;

	public final class PaintOrder {

		private byte type;

		private Screen screen;

		public PaintOrder(byte t, Screen s) {
			this.type = t;
			this.screen = s;
		}

		void paint(GLEx g) {
			switch (type) {
			case DRAW_USER:
				screen.draw(g);
				break;
			case DRAW_SPRITE:
				if (spriteRun) {
					sprites.createUI(g);
				} else if (spriteRun = (sprites != null && sprites.size() > 0)) {
					sprites.createUI(g);
				}
				break;
			case DRAW_DESKTOP:
				if (desktopRun) {
					desktop.createUI(g);
				} else if (desktopRun = (desktop != null && desktop.size() > 0)) {
					desktop.createUI(g);
				}
				break;
			case DRAW_STAGE:
				if (stageRun) {
					_players.paint(g);
				} else if (stageRun = (LSystem._process != null)
						&& (_players = LSystem._process.rootPlayer).children() > 0) {
					_players.paint(g);
				}
				break;
			case DRAW_EMPTY:
			default:
				break;
			}
		}

		void update(LTimerContext c) {
			switch (type) {
			case DRAW_USER:
				screen.alter(c);
				break;
			case DRAW_SPRITE:
				spriteRun = (sprites != null && sprites.size() > 0);
				if (spriteRun) {
					sprites.update(c.timeSinceLastUpdate);
				}
				break;
			case DRAW_DESKTOP:
				desktopRun = (desktop != null && desktop.size() > 0);
				if (desktopRun) {
					desktop.update(c.timeSinceLastUpdate);
				}
				break;
			case DRAW_STAGE:
				stageRun = (LSystem._process != null && (_players = LSystem._process.rootPlayer)
						.children() > 0);
				if (stageRun) {
					_players.update(c.timeSinceLastUpdate);
				}
				break;
			case DRAW_EMPTY:
			default:
				break;
			}
		}

	}

	public LGame getGame() {
		return LSystem._base;
	}

	private boolean spriteRun, desktopRun, stageRun;

	public final boolean isSpriteRunning() {
		return spriteRun;
	}

	public final boolean isDesktopRunning() {
		return desktopRun;
	}

	public final boolean isStageRunning() {
		return stageRun;
	}

	private boolean fristPaintFlag;

	private boolean secondPaintFlag;

	private boolean lastPaintFlag;

	private boolean basePaintFlag;

	public static enum SensorDirection {
		NONE, LEFT, RIGHT, UP, DOWN;
	}

	public static interface LEvent {

		public Screen call();

	}

	public abstract void draw(GLEx g);

	public final static int SCREEN_NOT_REPAINT = 0;

	public final static int SCREEN_TEXTURE_REPAINT = 1;

	public final static int SCREEN_COLOR_REPAINT = 2;

	// 0.3.2版新增的简易重力控制接口
	private GravityHandler gravityHandler;

	private LColor color;

	private float lastTouchX, lastTouchY, touchDX, touchDY;

	public long elapsedTime;

	private final static boolean[] touchType, keyType;

	private int touchButtonPressed = SysInput.NO_BUTTON,
			touchButtonReleased = SysInput.NO_BUTTON;

	private int keyButtonPressed = SysInput.NO_KEY,
			keyButtonReleased = SysInput.NO_KEY;

	boolean isNext;

	private int mode, frame;

	private boolean processing = true;

	private LTexture currentScreen;

	protected LProcess handler;

	private int width, height, halfWidth, halfHeight;

	private SensorDirection direction = SensorDirection.NONE;

	// 精灵集合
	private Sprites sprites;

	// 桌面集合
	private Desktop desktop;

	private PointI touch = new PointI(0, 0);

	private boolean isLoad, isLock, isClose, isTranslate, isGravity;

	private float tx, ty;

	// 舞台对象
	private PaintOrder baseOrder;

	// 首先绘制的对象
	private PaintOrder fristOrder;

	// 其次绘制的对象
	private PaintOrder secondOrder;

	// 最后绘制的对象
	private PaintOrder lastOrder;

	private PaintOrder userOrder, spriteOrder, desktopOrder, stageOrder;

	private TArray<RectBox> limits = new TArray<RectBox>(10);

	private boolean replaceLoading;

	private int replaceScreenSpeed = 8;

	private LTimer replaceDelay = new LTimer(0);

	private Screen replaceDstScreen;

	private EmptyObject dstPos = new EmptyObject();

	private MoveMethod replaceMethod = MoveMethod.FROM_LEFT;

	// Screen切换方式
	public static enum MoveMethod {
		FROM_LEFT, FROM_UP, FROM_DOWN, FROM_RIGHT, FROM_UPPER_LEFT, FROM_UPPER_RIGHT, FROM_LOWER_LEFT, FROM_LOWER_RIGHT, OUT_LEFT, OUT_UP, OUT_DOWN, OUT_RIGHT, OUT_UPPER_LEFT, OUT_UPPER_RIGHT, OUT_LOWER_LEFT, OUT_LOWER_RIGHT
	}

	private boolean isScreenFrom = false;

	public Screen replaceScreen(final Screen screen, MoveMethod m) {
		if (screen != null && screen != this) {
			screen.setOnLoadState(false);
			setLock(true);
			screen.setLock(true);
			this.replaceMethod = m;
			this.replaceDstScreen = screen;

			screen.setRepaintMode(SCREEN_NOT_REPAINT);
			switch (m) {
			case FROM_LEFT:
				dstPos.setLocation(-getWidth(), 0);
				isScreenFrom = true;
				break;
			case FROM_RIGHT:
				dstPos.setLocation(getWidth(), 0);
				isScreenFrom = true;
				break;
			case FROM_UP:
				dstPos.setLocation(0, -getHeight());
				isScreenFrom = true;
				break;
			case FROM_DOWN:
				dstPos.setLocation(0, getHeight());
				isScreenFrom = true;
				break;
			case FROM_UPPER_LEFT:
				dstPos.setLocation(-getWidth(), -getHeight());
				isScreenFrom = true;
				break;
			case FROM_UPPER_RIGHT:
				dstPos.setLocation(getWidth(), -getHeight());
				isScreenFrom = true;
				break;
			case FROM_LOWER_LEFT:
				dstPos.setLocation(-getWidth(), getHeight());
				isScreenFrom = true;
				break;
			case FROM_LOWER_RIGHT:
				dstPos.setLocation(getWidth(), getHeight());
				isScreenFrom = true;
				break;
			default:
				dstPos.setLocation(0, 0);
				isScreenFrom = false;
				break;
			}

			RealtimeProcessManager.get().addProcess(new RealtimeProcess() {

				@Override
				public void run(LTimerContext time) {
					screen.onCreate(LSystem.viewSize.getWidth(),
							LSystem.viewSize.getHeight());
					screen.setClose(false);
					screen.onLoad();
					screen.setRepaintMode(SCREEN_NOT_REPAINT);
					screen.onLoaded();
					screen.setOnLoadState(true);
					kill();
				}
			});

			replaceLoading = true;
		}

		return this;
	}

	public int getReplaceScreenSpeed() {
		return replaceScreenSpeed;
	}

	public Screen setReplaceScreenSpeed(int s) {
		this.replaceScreenSpeed = s;
		return this;
	}

	public Screen setReplaceScreenDelay(long d) {
		replaceDelay.setDelay(d);
		return this;
	}

	public long getReplaceScreenDelay() {
		return replaceDelay.getDelay();
	}

	private void submitReplaceScreen() {
		if (handler != null) {
			handler.setCurrentScreen(replaceDstScreen);
		}
		replaceLoading = false;
	}

	public Screen addTouchLimit(LObject c) {
		if (c != null) {
			limits.add(c.getCollisionArea());
		}
		return this;
	}

	public Screen addTouchLimit(RectBox r) {
		if (r != null) {
			limits.add(r);
		}
		return this;
	}

	public boolean isClickLimit(GameTouch e) {
		return isClickLimit(e.x(), e.y());
	}

	public boolean isClickLimit(int x, int y) {
		if (limits.size == 0) {
			return false;
		}
		for (RectBox rect : limits) {
			if (rect.contains(x, y)) {
				return true;
			}
		}
		return false;
	}

	private RectBox tempRect;

	public RectBox getBox() {
		if (tempRect == null) {
			tempRect = new RectBox(this.getX(), this.getY(), this.getWidth(),
					this.getHeight());
		} else {
			tempRect.setBounds(this.getX(), this.getY(), this.getWidth(),
					this.getHeight());
		}
		return tempRect;
	}

	protected final PaintOrder DRAW_USER_PAINT() {
		if (userOrder == null) {
			userOrder = new PaintOrder(DRAW_USER, this);
		}
		return userOrder;
	}

	protected final PaintOrder DRAW_STAGE_PAINT() {
		if (stageOrder == null) {
			stageOrder = new PaintOrder(DRAW_STAGE, this);
		}
		return stageOrder;
	}

	protected final PaintOrder DRAW_SPRITE_PAINT() {
		if (spriteOrder == null) {
			spriteOrder = new PaintOrder(DRAW_SPRITE, this);
		}
		return spriteOrder;
	}

	protected final PaintOrder DRAW_DESKTOP_PAINT() {
		if (desktopOrder == null) {
			desktopOrder = new PaintOrder(DRAW_DESKTOP, this);
		}
		return desktopOrder;
	}

	static {
		keyType = new boolean[255];
		touchType = new boolean[15];
	}

	public Screen() {
		resetBase();
	}

	final void resetBase() {
		LSTRDictionary.dispose();
		this.handler = LSystem._process;
		this.width = LSystem.viewSize.getWidth();
		this.height = LSystem.viewSize.getHeight();
		this.halfWidth = width / 2;
		this.halfHeight = height / 2;
		// 基础画布为舞台
		this.baseOrder = DRAW_STAGE_PAINT();
		// 最先精灵
		this.fristOrder = DRAW_SPRITE_PAINT();
		// 其次桌面
		this.secondOrder = DRAW_DESKTOP_PAINT();
		// 最后用户
		this.lastOrder = DRAW_USER_PAINT();
		this.fristPaintFlag = true;
		this.secondPaintFlag = true;
		this.lastPaintFlag = true;
		this.basePaintFlag = true;
	}

	public boolean contains(float x, float y) {
		return LSystem.viewSize.getRect().contains(x, y);
	}

	public boolean contains(float x, float y, float w, float h) {
		return LSystem.viewSize.getRect().contains(x, y, w, h);
	}

	public boolean intersects(float x, float y) {
		return LSystem.viewSize.getRect().intersects(x, y);
	}

	public boolean intersects(float x, float y, float w, float h) {
		return LSystem.viewSize.getRect().intersects(x, y, w, h);
	}

	/**
	 * 当Screen被创建(或再次加载)时将调用此函数
	 * 
	 * @param width
	 * @param height
	 */
	public void onCreate(int width, int height) {
		this.mode = SCREEN_NOT_REPAINT;
		this.width = width;
		this.height = height;
		this.halfWidth = width / 2;
		this.halfHeight = height / 2;
		this.lastTouchX = lastTouchY = touchDX = touchDY = 0;
		this.isLoad = isLock = isClose = isTranslate = isGravity = false;
		if (sprites != null) {
			sprites.close();
			sprites.removeAll();
			sprites = null;
		}
		this.sprites = new Sprites(this, width, height);
		if (desktop != null) {
			desktop.close();
			desktop.clear();
			desktop = null;
		}
		this.desktop = new Desktop(this, width, height);
		this.isNext = true;
	}

	public Screen addLoad(Updateable u) {
		if (handler != null) {
			handler.addLoad(u);
		}
		return this;
	}

	public Screen removeLoad(Updateable u) {
		if (handler != null) {
			handler.removeLoad(u);
		}
		return this;
	}

	public Screen removeAllLoad() {
		if (handler != null) {
			handler.removeAllLoad();
		}
		return this;
	}

	public Screen addUnLoad(Updateable u) {
		if (handler != null) {
			handler.addUnLoad(u);
		}
		return this;
	}

	public Screen removeUnLoad(Updateable u) {
		if (handler != null) {
			handler.removeUnLoad(u);
		}
		return this;
	}

	public Screen removeAllUnLoad() {
		if (handler != null) {
			handler.removeAllUnLoad();
		}
		return this;
	}

	/**
	 * 当执行Screen转换时将调用此函数(如果返回的LTransition不为null，则渐变效果会被执行)
	 * 
	 * @return
	 */
	public LTransition onTransition() {
		return _transition;
	}

	/**
	 * 注入一个渐变特效，在引入Screen时将使用此特效进行渐变.
	 * 
	 * @param t
	 * @return
	 */
	public Screen setTransition(LTransition t) {
		this._transition = t;
		return this;
	}

	/**
	 * 注入一个渐变特效的名称，以及渐变使用的颜色，在引入Screen时将使用此特效进行渐变.
	 * 
	 * @param transName
	 * @param c
	 * @return
	 */
	public Screen setTransition(String transName, LColor c) {
		this._transition = LTransition.newTransition(transName, c);
		return this;
	}

	/**
	 * @see onTransition
	 * 
	 * @return
	 */
	public LTransition getTransition() {
		return this._transition;
	}

	/**
	 * 设定重力系统是否启动
	 * 
	 * @param g
	 * @return
	 */
	public GravityHandler setGravity(boolean g) {
		if (g && gravityHandler == null) {
			gravityHandler = new GravityHandler();
		}
		this.isGravity = g;
		return gravityHandler;
	}

	/**
	 * 判断重力系统是否启动
	 * 
	 * @return
	 */
	public boolean isGravity() {
		return this.isGravity;
	}

	/**
	 * 获得当前重力器句柄
	 * 
	 * @return
	 */
	public GravityHandler getGravityHandler() {
		return setGravity(true);
	}

	/**
	 * 获得当前游戏事务运算时间是否被锁定
	 * 
	 * @return
	 */
	public boolean isLock() {
		return isLock;
	}

	/**
	 * 锁定游戏事务运算时间
	 * 
	 * @param lock
	 */
	public Screen setLock(boolean lock) {
		this.isLock = lock;
		return this;
	}

	/**
	 * 关闭游戏
	 * 
	 * @param close
	 */
	public Screen setClose(boolean close) {
		this.isClose = close;
		return this;
	}

	/**
	 * 判断游戏是否被关闭
	 * 
	 * @return
	 */
	public boolean isClose() {
		return isClose;
	}

	/**
	 * 设定当前帧
	 * 
	 * @param frame
	 */
	public Screen setFrame(int frame) {
		this.frame = frame;
		return this;
	}

	/**
	 * 返回当前帧
	 * 
	 * @return
	 */
	public int getFrame() {
		return frame;
	}

	/**
	 * 移动当前帧
	 * 
	 * @return
	 */
	public synchronized boolean next() {
		this.frame++;
		return isNext;
	}

	/**
	 * 初始化时加载的数据
	 */
	public abstract void onLoad();

	/**
	 * 初始化加载完毕
	 * 
	 */
	public void onLoaded() {

	}

	/**
	 * 改变资源加载状态
	 */
	public Screen setOnLoadState(boolean flag) {
		this.isLoad = flag;
		return this;
	}

	/**
	 * 是否处于过渡中
	 * 
	 * @return
	 */
	public boolean isTransitioning() {
		if (handler != null) {
			return handler.isTransitioning();
		}
		// 如果过渡效果不存在，则返回是否加载完毕
		return isLoad;
	}

	/**
	 * 过度是否完成
	 */
	public boolean isTransitionCompleted() {
		if (handler != null) {
			return handler.isTransitionCompleted();
		}
		// 如果过渡效果不存在，则返回是否加载完毕
		return isLoad;
	}

	/**
	 * 获得当前资源加载是否完成
	 */
	public boolean isOnLoadComplete() {
		return isLoad;
	}

	/**
	 * 取出第一个Screen并执行
	 * 
	 */
	public Screen runFirstScreen() {
		if (handler != null) {
			handler.runFirstScreen();
		}
		return this;
	}

	/**
	 * 取出最后一个Screen并执行
	 */
	public Screen runLastScreen() {
		if (handler != null) {
			handler.runLastScreen();
		}
		return this;
	}

	/**
	 * 运行指定位置的Screen
	 * 
	 * @param index
	 */
	public Screen runIndexScreen(int index) {
		if (handler != null) {
			handler.runIndexScreen(index);
		}
		return this;
	}

	/**
	 * 运行自当前Screen起的上一个Screen
	 */
	public Screen runPreviousScreen() {
		if (handler != null) {
			handler.runPreviousScreen();
		}
		return this;
	}

	/**
	 * 运行自当前Screen起的下一个Screen
	 */
	public Screen runNextScreen() {
		if (handler != null) {
			handler.runNextScreen();
		}
		return this;
	}

	/**
	 * 添加指定名称的Screen到当前Screen，但不立刻执行
	 * 
	 * @param name
	 * @param screen
	 * @return
	 */
	public Screen addScreen(CharSequence name, Screen screen) {
		if (handler != null) {
			handler.addScreen(name, screen);
		}
		return this;
	}

	/**
	 * 获得指定名称的Screen
	 * 
	 * @param name
	 * @return
	 */
	public Screen getScreen(CharSequence name) {
		if (handler != null) {
			return handler.getScreen(name);
		}
		return this;
	}

	/**
	 * 执行指定名称的Screen
	 * 
	 * @param name
	 * @return
	 */
	public Screen runScreen(CharSequence name) {
		if (handler != null) {
			return handler.runScreen(name);
		}
		return this;
	}

	public Screen clearScreen() {
		if (handler != null) {
			handler.clearScreens();
		}
		return this;
	}

	/**
	 * 向缓存中添加Screen数据，但是不立即执行
	 * 
	 * @param screen
	 */
	public Screen addScreen(Screen screen) {
		if (handler != null) {
			handler.addScreen(screen);
		}
		return this;
	}

	/**
	 * 获得保存的Screen列表
	 * 
	 * @return
	 */
	public TArray<Screen> getScreens() {
		if (handler != null) {
			return handler.getScreens();
		}
		return null;
	}

	/**
	 * 获得缓存的Screen总数
	 */
	public int getScreenCount() {
		if (handler != null) {
			return handler.getScreenCount();
		}
		return 0;
	}

	/**
	 * 返回精灵监听
	 * 
	 * @return
	 */

	public SpriteListener getSprListerner() {
		if (sprites == null) {
			return null;
		}
		return sprites.getSprListerner();
	}

	/**
	 * 监听Screen中精灵
	 * 
	 * @param sprListerner
	 */

	public Screen setSprListerner(SpriteListener sprListerner) {
		if (sprites == null) {
			return this;
		}
		sprites.setSprListerner(sprListerner);
		return this;
	}

	/**
	 * 获得当前Screen类名
	 */
	public String getName() {
		return getClass().getSimpleName();
	}

	/**
	 * 设定模拟按钮监听器
	 */

	public Screen setEmulatorListener(EmulatorListener emulator) {
		if (LSystem._process != null) {
			LSystem._process.setEmulatorListener(emulator);
		}
		return this;
	}

	/**
	 * 返回模拟按钮集合
	 * 
	 * @return
	 */

	public EmulatorButtons getEmulatorButtons() {
		if (LSystem._process != null) {
			return LSystem._process.getEmulatorButtons();
		}
		return null;
	}

	/**
	 * 设定模拟按钮组是否显示
	 * 
	 * @param visible
	 */

	public Screen emulatorButtonsVisible(boolean visible) {
		if (LSystem._process != null) {
			try {
				EmulatorButtons es = LSystem._process.getEmulatorButtons();
				es.setVisible(visible);
			} catch (Exception e) {
			}
		}
		return this;
	}

	/**
	 * 设定背景图像
	 * 
	 * @param screen
	 */
	public Screen setBackground(LTexture background) {
		if (background != null) {
			setRepaintMode(SCREEN_TEXTURE_REPAINT);
			LTexture screen = background;
			LTexture tmp = currentScreen;
			currentScreen = screen;
			if (tmp != null) {
				tmp.close();
				tmp = null;
			}
		} else {
			setRepaintMode(SCREEN_NOT_REPAINT);
		}
		return this;
	}

	/**
	 * 设定背景图像
	 */
	public Screen setBackground(String fileName) {
		return this.setBackground(LTextures.loadTexture(fileName));
	}

	/**
	 * 设定背景颜色
	 * 
	 * @param c
	 */
	public Screen setBackground(LColor c) {
		setRepaintMode(SCREEN_COLOR_REPAINT);
		if (color == null) {
			color = new LColor(c);
		} else {
			color.setColor(c.r, c.g, c.b, c.a);
		}
		return this;
	}

	public LColor getColor() {
		return color;
	}

	/**
	 * 返回背景图像
	 * 
	 * @return
	 */
	public LTexture getBackground() {
		return currentScreen;
	}

	public Desktop getDesktop() {
		return desktop;
	}

	public Sprites getSprites() {
		return sprites;
	}

	/**
	 * 返回位于屏幕顶部的组件
	 * 
	 * @return
	 */

	public LComponent getTopComponent() {
		if (desktop != null) {
			return desktop.getTopComponent();
		}
		return null;
	}

	/**
	 * 返回位于屏幕底部的组件
	 * 
	 * @return
	 */

	public LComponent getBottomComponent() {
		if (desktop != null) {
			return desktop.getBottomComponent();
		}
		return null;
	}

	public LLayer getTopLayer() {
		if (desktop != null) {
			return desktop.getTopLayer();
		}
		return null;
	}

	public LLayer getBottomLayer() {
		if (desktop != null) {
			return desktop.getBottomLayer();
		}
		return null;
	}

	/**
	 * 返回位于数据顶部的精灵
	 * 
	 */

	public ISprite getTopSprite() {
		if (sprites != null) {
			return sprites.getTopSprite();
		}
		return null;
	}

	/**
	 * 返回位于数据底部的精灵
	 * 
	 */

	public ISprite getBottomSprite() {
		if (sprites != null) {
			return sprites.getBottomSprite();
		}
		return null;
	}

	public Screen add(Object... obj) {
		for (int i = 0; i < obj.length; i++) {
			add(obj[i]);
		}
		return this;
	}

	/**
	 * 添加游戏对象
	 * 
	 * @param obj
	 * @return
	 */
	public Screen add(Object obj) {
		if (obj instanceof ISprite) {
			add((ISprite) obj);
		} else if (obj instanceof LComponent) {
			add((LComponent) obj);
		} else if (obj instanceof Player) {
			add((Player) obj);
		} else if (obj instanceof Stage) {
			puspStage((Stage) obj);
		} else if (obj instanceof Updateable) {
			addLoad((Updateable) obj);
		} else if (obj instanceof GameProcess) {
			addProcess((GameProcess) obj);
		} else if (obj instanceof LRelease) {
			putRelease((LRelease) obj);
		}
		return this;
	}

	/**
	 * 删除指定对象
	 * 
	 * @param obj
	 * @return
	 */
	public Screen remove(Object obj) {
		if (obj instanceof ISprite) {
			remove((ISprite) obj);
		} else if (obj instanceof LComponent) {
			remove((LComponent) obj);
		} else if (obj instanceof Player) {
			remove((Player) obj);
		} else if (obj instanceof Stage) {
			popTo((Stage) obj);
		} else if (obj instanceof Updateable) {
			removeLoad((Updateable) obj);
		} else if (obj instanceof GameProcess) {
			removeProcess((GameProcess) obj);
		} else if (obj instanceof LRelease) {
			removeRelease((LRelease) obj);
		}
		return this;
	}

	public Screen remove(Object... obj) {
		for (int i = 0; i < obj.length; i++) {
			remove(obj[i]);
		}
		return this;
	}

	public Screen add(Player player) {
		if (LSystem._process != null && LSystem._process.rootPlayer != null) {
			LSystem._process.rootPlayer.add(player);
			if (player instanceof LTouchArea) {
				registerTouchArea((LTouchArea) player);
			}
		}
		return this;
	}

	public Screen remove(Player player) {
		if (LSystem._process != null && LSystem._process.rootPlayer != null) {
			LSystem._process.rootPlayer.remove(player);
			if (player instanceof LTouchArea) {
				unregisterTouchArea((LTouchArea) player);
			}
		}
		return this;
	}

	/**
	 * 添加游戏精灵
	 * 
	 * @param sprite
	 */

	public Screen add(ISprite sprite) {
		if (sprites != null) {
			sprites.add(sprite);
			if (sprite instanceof LTouchArea) {
				registerTouchArea((LTouchArea) sprite);
			}
		}
		return this;
	}

	/**
	 * 添加游戏组件
	 * 
	 * @param comp
	 */

	public Screen add(LComponent comp) {
		if (desktop != null) {
			desktop.add(comp);
			if (comp instanceof LTouchArea) {
				registerTouchArea((LTouchArea) comp);
			}
		}
		return this;
	}

	public boolean contains(ISprite sprite) {
		if (sprites != null) {
			return sprites.contains(sprite);
		}
		return false;
	}

	public Screen remove(ISprite sprite) {
		if (sprites != null) {
			sprites.remove(sprite);
			if (sprite instanceof LTouchArea) {
				unregisterTouchArea((LTouchArea) sprite);
			}
		}
		return this;
	}

	public boolean contains(LComponent sprite) {
		if (desktop != null) {
			return desktop.contains(sprite);
		}
		return false;
	}

	public Screen remove(LComponent comp) {
		if (desktop != null) {
			desktop.remove(comp);
			if (comp instanceof LTouchArea) {
				unregisterTouchArea((LTouchArea) comp);
			}
		}
		return this;
	}

	public boolean contains(Object obj) {
		if (obj instanceof ISprite) {
			return contains((ISprite) obj);
		} else if (obj instanceof LComponent) {
			return contains((LComponent) obj);
		}
		return false;
	}

	public Screen removeAll() {
		if (sprites != null) {
			sprites.removeAll();
		}
		if (desktop != null) {
			desktop.getContentPane().clear();
		}
		if (LSystem._process != null) {
			if (LSystem._process.rootPlayer != null) {
				LSystem._process.rootPlayer.removeAll();
			}
			if (LSystem._process.stageSystem != null) {
				LSystem._process.stageSystem.removeAll();
			}
		}
		ActionControl.get().clear();
		removeAllLoad();
		removeAllUnLoad();
		clearTouchAreas();
		return this;
	}

	/**
	 * 判断是否点中指定精灵
	 * 
	 * @param sprite
	 * @return
	 */

	public boolean onClick(ISprite sprite) {
		if (sprite == null) {
			return false;
		}
		if (sprite.isVisible()) {
			RectBox rect = sprite.getCollisionBox();
			if (rect.contains(SysTouch.getX(), SysTouch.getY())
					|| rect.intersects(SysTouch.getX(), SysTouch.getY())) {
				return true;
			}
		}
		return false;
	}

	/**
	 * 判断是否点中指定组件
	 * 
	 * @param component
	 * @return
	 */

	public boolean onClick(LComponent component) {
		if (component == null) {
			return false;
		}
		if (component.isVisible()) {
			RectBox rect = component.getCollisionBox();
			if (rect.contains(SysTouch.getX(), SysTouch.getY())
					|| rect.intersects(SysTouch.getX(), SysTouch.getY())) {
				return true;
			}
		}
		return false;
	}

	public Screen centerOn(final LObject object) {
		LObject.centerOn(object, getWidth(), getHeight());
		return this;
	}

	public Screen topOn(final LObject object) {
		LObject.topOn(object, getWidth(), getHeight());
		return this;
	}

	public Screen leftOn(final LObject object) {
		LObject.leftOn(object, getWidth(), getHeight());
		return this;
	}

	public Screen rightOn(final LObject object) {
		LObject.rightOn(object, getWidth(), getHeight());
		return this;
	}

	public Screen bottomOn(final LObject object) {
		LObject.bottomOn(object, getWidth(), getHeight());
		return this;
	}

	/**
	 * 获得背景显示模式
	 */
	public int getRepaintMode() {
		return mode;
	}

	/**
	 * 设定背景刷新模式
	 * 
	 * @param mode
	 */
	public void setRepaintMode(int mode) {
		this.mode = mode;
	}

	public Screen setLocation(float x, float y) {
		this.tx = x;
		this.ty = y;
		this.isTranslate = (tx != 0 || ty != 0);
		return this;
	}

	public Screen setX(float x) {
		setLocation(x, ty);
		return this;
	}

	public Screen setY(float y) {
		setLocation(tx, y);
		return this;
	}

	@Override
	public float getX() {
		return this.tx;
	}

	@Override
	public float getY() {
		return this.ty;
	}

	protected LTextureImage createTextureImage(float width, float height) {
		if (LSystem._base == null) {
			return null;
		}
		return new LTextureImage(LSystem._base.graphics(), LSystem._base
				.display().GL().batch(), width, height, true);
	}

	protected void afterUI(GLEx g) {

	}

	protected void beforeUI(GLEx g) {

	}

	private final void repaint(GLEx g) {
		if (!isClose) {
			try {
				// 记录屏幕矩阵以及画笔
				g.save();
				// 偏移屏幕
				if (isTranslate) {
					g.translate(tx, ty);
				}
				if (_isExistCamera) {
					g.setCamera(_baseCamera);
				}
				// 最下一层渲染，可重载
				afterUI(g);
				// PS:下列四项允许用户调整顺序
				// 基础
				if (basePaintFlag) {
					baseOrder.paint(g);
				}
				// 精灵
				if (fristPaintFlag) {
					fristOrder.paint(g);
				}
				// 其次，桌面
				if (secondPaintFlag) {
					secondOrder.paint(g);
				}
				// 最后，用户渲染
				if (lastPaintFlag) {
					lastOrder.paint(g);
				}
				// 最前一层渲染，可重载
				beforeUI(g);
			} finally {
				// 若存在摄影机,则还原camera坐标
				if (_isExistCamera) {
					g.restoreTx();
				}
				// 还原屏幕矩阵以及画笔
				g.restore();
			}
		}
	}

	private int tmpColor = LColor.DEF_COLOR;

	public synchronized void createUI(GLEx g) {
		if (isClose) {
			return;
		}
		if (replaceLoading) {
			if (replaceDstScreen == null
					|| !replaceDstScreen.isOnLoadComplete()) {
				repaint(g);
			} else if (replaceDstScreen.isOnLoadComplete()) {
				if (isScreenFrom) {
					repaint(g);
					if (replaceDstScreen.color != null) {
						tmpColor = g.color();
						g.setColor(replaceDstScreen.color);
						g.fillRect(dstPos.x(), dstPos.y(), getWidth(),
								getHeight());
						g.setColor(tmpColor);
					}
					if (replaceDstScreen.currentScreen != null) {
						g.draw(replaceDstScreen.currentScreen, dstPos.x(),
								dstPos.y(), getWidth(), getHeight());
					}
					if (dstPos.x() != 0 || dstPos.y() != 0) {
						g.setClip(dstPos.x(), dstPos.y(), getWidth(),
								getHeight());
						g.translate(dstPos.x(), dstPos.y());
					}
					replaceDstScreen.createUI(g);
					if (dstPos.x() != 0 || dstPos.y() != 0) {
						g.translate(-dstPos.x(), -dstPos.y());
						g.clearClip();
					}
				} else {
					if (replaceDstScreen.color != null) {
						tmpColor = g.color();
						g.setColor(replaceDstScreen.color);
						g.fillRect(0, 0, getWidth(), getHeight());
						g.setColor(tmpColor);
					}
					if (replaceDstScreen.currentScreen != null) {
						g.draw(replaceDstScreen.currentScreen, 0, 0,
								getWidth(), getHeight());
					}
					replaceDstScreen.createUI(g);
					if (color != null) {
						tmpColor = g.color();
						g.setColor(color);
						g.fillRect(dstPos.x(), dstPos.y(), getWidth(),
								getHeight());
						g.setColor(tmpColor);
					}
					if (getBackground() != null) {
						g.draw(currentScreen, dstPos.x(), dstPos.y(),
								getWidth(), getHeight());
					}
					if (dstPos.x() != 0 || dstPos.y() != 0) {
						g.setClip(dstPos.x(), dstPos.y(), getWidth(),
								getHeight());
						g.translate(dstPos.x(), dstPos.y());
					}
					repaint(g);
					if (dstPos.x() != 0 || dstPos.y() != 0) {
						g.translate(-dstPos.x(), -dstPos.y());
						g.clearClip();
					}
				}
			}
		} else {
			repaint(g);
		}
	}

	private final void process(final LTimerContext timer) {
		this.elapsedTime = timer.timeSinceLastUpdate;
		if (processing && !isClose) {
			if (isGravity) {
				gravityHandler.update(elapsedTime);
			}
			if (basePaintFlag) {
				baseOrder.update(timer);
			}
			if (fristPaintFlag) {
				fristOrder.update(timer);
			}
			if (secondPaintFlag) {
				secondOrder.update(timer);
			}
			if (lastPaintFlag) {
				lastOrder.update(timer);
			}
		}
		this.touchDX = SysTouch.getX() - lastTouchX;
		this.touchDY = SysTouch.getY() - lastTouchY;
		this.lastTouchX = SysTouch.getX();
		this.lastTouchY = SysTouch.getY();
		this.touchButtonReleased = NO_BUTTON;
	}

	public void runTimer(final LTimerContext timer) {
		if (isClose) {
			return;
		}
		if (replaceLoading) {
			if (replaceDstScreen == null
					|| !replaceDstScreen.isOnLoadComplete()) {
				process(timer);
			} else if (replaceDstScreen.isOnLoadComplete()) {
				process(timer);
				if (replaceDelay.action(timer)) {
					switch (replaceMethod) {
					case FROM_LEFT:
						dstPos.move_right(replaceScreenSpeed);
						if (dstPos.x() >= 0) {
							submitReplaceScreen();
							return;
						}
						break;
					case FROM_RIGHT:
						dstPos.move_left(replaceScreenSpeed);
						if (dstPos.x() <= 0) {
							submitReplaceScreen();
							return;
						}
						break;
					case FROM_UP:
						dstPos.move_down(replaceScreenSpeed);
						if (dstPos.y() >= 0) {
							submitReplaceScreen();
							return;
						}
						break;
					case FROM_DOWN:
						dstPos.move_up(replaceScreenSpeed);
						if (dstPos.y() <= 0) {
							submitReplaceScreen();
							return;
						}
						break;
					case OUT_LEFT:
						dstPos.move_left(replaceScreenSpeed);
						if (dstPos.x() < -getWidth()) {
							submitReplaceScreen();
							return;
						}
						break;
					case OUT_RIGHT:
						dstPos.move_right(replaceScreenSpeed);
						if (dstPos.x() > getWidth()) {
							submitReplaceScreen();
							return;
						}
						break;
					case OUT_UP:
						dstPos.move_up(replaceScreenSpeed);
						if (dstPos.y() < -getHeight()) {
							submitReplaceScreen();
							return;
						}
						break;
					case OUT_DOWN:
						dstPos.move_down(replaceScreenSpeed);
						if (dstPos.y() > getHeight()) {
							submitReplaceScreen();
							return;
						}
						break;
					case FROM_UPPER_LEFT:
						if (dstPos.y() < 0) {
							dstPos.move_45D_right(replaceScreenSpeed);
						} else {
							dstPos.move_right(replaceScreenSpeed);
						}
						if (dstPos.y() >= 0 && dstPos.x() >= 0) {
							submitReplaceScreen();
							return;
						}
						break;
					case FROM_UPPER_RIGHT:
						if (dstPos.y() < 0) {
							dstPos.move_45D_down(replaceScreenSpeed);
						} else {
							dstPos.move_left(replaceScreenSpeed);
						}
						if (dstPos.y() >= 0 && dstPos.x() <= 0) {
							submitReplaceScreen();
							return;
						}
						break;
					case FROM_LOWER_LEFT:
						if (dstPos.y() > 0) {
							dstPos.move_45D_up(replaceScreenSpeed);
						} else {
							dstPos.move_right(replaceScreenSpeed);
						}
						if (dstPos.y() <= 0 && dstPos.x() >= 0) {
							submitReplaceScreen();
							return;
						}
						break;
					case FROM_LOWER_RIGHT:
						if (dstPos.y() > 0) {
							dstPos.move_45D_left(replaceScreenSpeed);
						} else {
							dstPos.move_left(replaceScreenSpeed);
						}
						if (dstPos.y() <= 0 && dstPos.x() <= 0) {
							submitReplaceScreen();
							return;
						}
						break;
					case OUT_UPPER_LEFT:
						dstPos.move_45D_left(replaceScreenSpeed);
						if (dstPos.x() < -getWidth()
								|| dstPos.y() <= -getHeight()) {
							submitReplaceScreen();
							return;
						}
						break;
					case OUT_UPPER_RIGHT:
						dstPos.move_45D_up(replaceScreenSpeed);
						if (dstPos.x() > getWidth()
								|| dstPos.y() < -getHeight()) {
							submitReplaceScreen();
							return;
						}
						break;
					case OUT_LOWER_LEFT:
						dstPos.move_45D_down(replaceScreenSpeed);
						if (dstPos.x() < -getWidth()
								|| dstPos.y() > getHeight()) {
							submitReplaceScreen();
							return;
						}
						break;
					case OUT_LOWER_RIGHT:
						dstPos.move_45D_right(replaceScreenSpeed);
						if (dstPos.x() > getWidth() || dstPos.y() > getHeight()) {
							submitReplaceScreen();
							return;
						}
						break;
					default:
						break;
					}
					replaceDstScreen.runTimer(timer);
				}
			}
		} else {
			process(timer);
		}
	}

	public SysInput getInput() {
		return this;
	}

	public Screen setNext(boolean next) {
		this.isNext = next;
		return this;
	}

	public abstract void alter(LTimerContext timer);

	/**
	 * 设定游戏窗体
	 */
	public Screen setScreen(Screen screen) {
		if (handler != null) {
			this.handler.setScreen(screen);
		}
		return this;
	}

	@Override
	public int getWidth() {
		return width;
	}

	@Override
	public int getHeight() {
		return height;
	}

	/**
	 * 刷新基础设置
	 */
	@Override
	public void refresh() {
		for (int i = 0; i < touchType.length; i++) {
			touchType[i] = false;
		}
		touchDX = touchDY = 0;
		for (int i = 0; i < keyType.length; i++) {
			keyType[i] = false;
		}
	}

	public abstract void resize(int width, int height);

	@Override
	public PointI getTouch() {
		touch.set((int) SysTouch.getX(), (int) SysTouch.getY());
		return touch;
	}

	public boolean isPaused() {
		return LSystem.PAUSED;
	}

	@Override
	public int getTouchPressed() {
		return touchButtonPressed > SysInput.NO_BUTTON ? touchButtonPressed
				: SysInput.NO_BUTTON;
	}

	@Override
	public int getTouchReleased() {
		return touchButtonReleased > SysInput.NO_BUTTON ? touchButtonReleased
				: SysInput.NO_BUTTON;
	}

	@Override
	public boolean isTouchPressed(int button) {
		return touchButtonPressed == button;
	}

	@Override
	public boolean isTouchReleased(int button) {
		return touchButtonReleased == button;
	}

	@Override
	public int getTouchX() {
		return (int) SysTouch.getX();
	}

	@Override
	public int getTouchY() {
		return (int) SysTouch.getY();
	}

	@Override
	public int getTouchDX() {
		return (int) touchDX;
	}

	@Override
	public int getTouchDY() {
		return (int) touchDY;
	}

	@Override
	public boolean isTouchType(int type) {
		return touchType[type];
	}

	@Override
	public int getKeyPressed() {
		return keyButtonPressed > SysInput.NO_KEY ? keyButtonPressed
				: SysInput.NO_KEY;
	}

	@Override
	public boolean isKeyPressed(int keyCode) {
		return keyButtonPressed == keyCode;
	}

	@Override
	public int getKeyReleased() {
		return keyButtonReleased > SysInput.NO_KEY ? keyButtonReleased
				: SysInput.NO_KEY;
	}

	@Override
	public boolean isKeyReleased(int keyCode) {
		return keyButtonReleased == keyCode;
	}

	@Override
	public boolean isKeyType(int type) {
		return keyType[type];
	}

	public final void keyPressed(GameKey e) {
		if (isLock || isClose || !isLoad) {
			return;
		}
		int type = e.getType();
		int code = e.getKeyCode();
		try {
			this.onKeyDown(e);
			keyType[type] = true;
			keyButtonPressed = code;
			keyButtonReleased = SysInput.NO_KEY;
		} catch (Exception ex) {
			keyButtonPressed = SysInput.NO_KEY;
			keyButtonReleased = SysInput.NO_KEY;
			ex.printStackTrace();
		}
	}

	/**
	 * 设置键盘按下事件
	 * 
	 * @param code
	 */
	public void setKeyDown(int button) {
		try {
			keyButtonPressed = button;
			keyButtonReleased = SysInput.NO_KEY;
		} catch (Exception e) {
		}
	}

	public final void keyReleased(GameKey e) {
		if (isLock || isClose || !isLoad) {
			return;
		}
		int type = e.getType();
		int code = e.getKeyCode();
		try {
			this.onKeyUp(e);
			keyType[type] = false;
			keyButtonReleased = code;
			keyButtonPressed = SysInput.NO_KEY;
		} catch (Exception ex) {
			keyButtonPressed = SysInput.NO_KEY;
			keyButtonReleased = SysInput.NO_KEY;
			ex.printStackTrace();
		}
	}

	@Override
	public void setKeyUp(int button) {
		try {
			keyButtonReleased = button;
			keyButtonPressed = SysInput.NO_KEY;
		} catch (Exception e) {
		}
	}

	public void keyTyped(GameKey e) {
		if (isLock || isClose || !isLoad) {
			return;
		}
		onKeyTyped(e);
	}

	public void onKeyDown(GameKey e) {

	}

	public void onKeyUp(GameKey e) {

	}

	public void onKeyTyped(GameKey e) {

	}

	public final void mousePressed(GameTouch e) {
		if (isLock || isClose || !isLoad) {
			return;
		}
		if (isTranslate) {
			e.offset(tx, ty);
		}
		int type = e.getType();
		int button = e.getButton();

		updateTouchArea(Event.DOWN, e.getX(), e.getY());

		try {
			touchType[type] = true;
			touchButtonPressed = button;
			touchButtonReleased = SysInput.NO_BUTTON;
			if (!isClickLimit(e)) {
				touchDown(e);
			}
		} catch (Exception ex) {
			touchButtonPressed = SysInput.NO_BUTTON;
			touchButtonReleased = SysInput.NO_BUTTON;
			ex.printStackTrace();
		}
	}

	public abstract void touchDown(GameTouch e);

	public void mouseReleased(GameTouch e) {
		if (isLock || isClose || !isLoad) {
			return;
		}
		if (isTranslate) {
			e.offset(tx, ty);
		}
		int type = e.getType();
		int button = e.getButton();

		updateTouchArea(Event.UP, e.getX(), e.getY());

		try {
			touchType[type] = false;
			touchButtonReleased = button;
			touchButtonPressed = SysInput.NO_BUTTON;
			if (!isClickLimit(e)) {
				touchUp(e);
			}
		} catch (Exception ex) {
			touchButtonPressed = SysInput.NO_BUTTON;
			touchButtonReleased = SysInput.NO_BUTTON;
			ex.printStackTrace();
		}
	}

	public abstract void touchUp(GameTouch e);

	public void mouseMoved(GameTouch e) {
		if (isLock || isClose || !isLoad) {
			return;
		}
		if (isTranslate) {
			e.offset(tx, ty);
		}

		updateTouchArea(Event.MOVE, e.getX(), e.getY());

		if (!isClickLimit(e)) {
			touchMove(e);
		}
	}

	public abstract void touchMove(GameTouch e);

	public void mouseDragged(GameTouch e) {
		if (isLock || isClose || !isLoad) {
			return;
		}
		if (isTranslate) {
			e.offset(tx, ty);
		}

		updateTouchArea(Event.DRAG, e.getX(), e.getY());

		if (!isClickLimit(e)) {
			touchDrag(e);
		}
	}

	public abstract void touchDrag(GameTouch e);

	/**
	 * 判定是否点击了指定位置
	 * 
	 * @param event
	 * @param x
	 * @param y
	 * @param width
	 * @param height
	 * @return
	 */
	public boolean inBounds(GameTouch event, float x, float y, float width,
			float height) {
		return (event.x() > x && event.x() < x + width - 1 && event.y() > y && event
				.y() < y + height - 1);
	}

	/**
	 * 判定是否点击了指定位置
	 * 
	 * @param event
	 * @param x
	 * @param y
	 * @param width
	 * @param height
	 * @return
	 */
	public boolean inBounds(LTouchLocation event, float x, float y,
			float width, float height) {
		return (event.x() > x && event.x() < x + width - 1 && event.y() > y && event
				.y() < y + height - 1);
	}

	/**
	 * 判定是否点击了指定位置
	 * 
	 * @param event
	 * @param o
	 * @return
	 */
	public boolean inBounds(GameTouch event, LObject o) {
		RectBox rect = o.getCollisionArea();
		if (rect != null) {
			return inBounds(event, rect);
		} else {
			return false;
		}
	}

	/**
	 * 判定是否点击了指定位置
	 * 
	 * @param event
	 * @param o
	 * @return
	 */
	public boolean inBounds(GameTouch event, ISprite o) {
		return inBounds(event, o.x(), o.y(), o.getWidth(), o.getHeight());
	}

	/**
	 * 判定是否点击了指定位置
	 * 
	 * @param event
	 * @param o
	 * @return
	 */
	public boolean inBounds(GameTouch event, LComponent o) {
		return inBounds(event, o.x(), o.y(), o.getWidth(), o.getHeight());
	}

	/**
	 * 判定是否点击了指定位置
	 * 
	 * @param event
	 * @param rect
	 * @return
	 */
	public boolean inBounds(GameTouch event, RectBox rect) {
		return inBounds(event, rect.x, rect.y, rect.width, rect.height);
	}

	@Override
	public boolean isMoving() {
		return SysTouch.isDrag();
	}

	public int getHalfWidth() {
		return halfWidth;
	}

	public int getHalfHeight() {
		return halfHeight;
	}

	public SensorDirection getSensorDirection() {
		return direction;
	}

	public PaintOrder getBaseOrder() {
		return baseOrder;
	}

	public Screen setBaseOrder(PaintOrder bOrder) {
		if (baseOrder == null) {
			this.basePaintFlag = false;
		} else {
			this.basePaintFlag = true;
			this.baseOrder = bOrder;
		}
		return this;
	}

	public PaintOrder getFristOrder() {
		return fristOrder;
	}

	public Screen setFristOrder(PaintOrder fristOrder) {
		if (fristOrder == null) {
			this.fristPaintFlag = false;
		} else {
			this.fristPaintFlag = true;
			this.fristOrder = fristOrder;
		}
		return this;
	}

	public PaintOrder getSecondOrder() {
		return secondOrder;
	}

	public Screen setSecondOrder(PaintOrder secondOrder) {
		if (secondOrder == null) {
			this.secondPaintFlag = false;
		} else {
			this.secondPaintFlag = true;
			this.secondOrder = secondOrder;
		}
		return this;
	}

	public PaintOrder getLastOrder() {
		return lastOrder;
	}

	public Screen setLastOrder(PaintOrder lastOrder) {
		if (lastOrder == null) {
			this.lastPaintFlag = false;
		} else {
			this.lastPaintFlag = true;
			this.lastOrder = lastOrder;
		}
		return this;
	}

	public final void destroy() {
		synchronized (this) {
			limits.clear();
			_touchAreas.clear();
			touchButtonPressed = SysInput.NO_BUTTON;
			touchButtonReleased = SysInput.NO_BUTTON;
			keyButtonPressed = SysInput.NO_KEY;
			keyButtonReleased = SysInput.NO_KEY;
			replaceLoading = false;
			replaceDelay.setDelay(10);
			tx = ty = 0;
			isClose = true;
			isTranslate = false;
			isNext = false;
			isGravity = false;
			isLock = true;
			_isExistCamera = false;
			if (sprites != null) {
				sprites.close();
				sprites.clear();
				sprites = null;
			}
			if (desktop != null) {
				desktop.close();
				desktop.clear();
				desktop = null;
			}
			if (_players != null) {
				_players.close();
			}
			if (LSystem._process != null && LSystem._process.rootPlayer != null) {
				LSystem._process.rootPlayer.close();
			}
			if (LSystem._process != null
					&& LSystem._process.stageSystem != null) {
				LSystem._process.stageSystem.close();
			}
			if (currentScreen != null) {
				LTexture parent = LTexture.firstFather(currentScreen);
				parent.closeChildAll();
				parent.close();
				currentScreen = null;
			}
			if (gravityHandler != null) {
				gravityHandler.close();
				gravityHandler = null;
			}
			if (releases != null) {
				for (LRelease r : releases) {
					if (r != null) {
						r.close();
					}
				}
				releases.clear();
			}
			_conns.close();
			release();
			close();
		}
	}

	public Display getDisplay() {
		return LSystem._base.display();
	}

	public RootPlayer getRootPlayer() {
		return LSystem._process.rootPlayer;
	}

	public StageSystem getStageSystem() {
		return LSystem._process.stageSystem;
	}

	public Screen puspStageUp(Stage stage) {
		if (LSystem._process != null) {
			LSystem._process.stageSystem.push(stage,
					LSystem._process.stageSystem.newSlide().up());
		}
		return this;
	}

	public Screen puspStageRight(Stage stage) {
		if (LSystem._process != null) {
			LSystem._process.stageSystem.push(stage,
					LSystem._process.stageSystem.newSlide().right());
		}
		return this;
	}

	public Screen puspStageLeft(Stage stage) {
		if (LSystem._process != null) {
			LSystem._process.stageSystem.push(stage,
					LSystem._process.stageSystem.newSlide().left());
		}
		return this;
	}

	public Screen puspStageDown(Stage stage) {
		if (LSystem._process != null) {
			LSystem._process.stageSystem.push(stage,
					LSystem._process.stageSystem.newSlide().down());
		}
		return this;
	}

	public Screen puspStage(Stage stage) {
		if (LSystem._process != null) {
			LSystem._process.stageSystem.push(stage);
		}
		return this;
	}

	public Screen puspStage(Stage stage, StageTransition trans) {
		if (LSystem._process != null) {
			LSystem._process.stageSystem.push(stage, trans);
		}
		return this;
	}

	public Screen puspStage(Iterable<? extends Stage> stages) {
		if (LSystem._process != null) {
			LSystem._process.stageSystem.push(stages);
		}
		return this;
	}

	public Screen puspStage(Iterable<? extends Stage> stages,
			StageTransition trans) {
		if (LSystem._process != null) {
			LSystem._process.stageSystem.push(stages, trans);
		}
		return this;
	}

	public Screen popTo(Stage newTopStage) {
		if (LSystem._process != null) {
			LSystem._process.stageSystem.popTo(newTopStage);
		}
		return this;
	}

	public Screen popTo(Stage newTopStage, StageTransition trans) {
		if (LSystem._process != null) {
			LSystem._process.stageSystem.popTo(newTopStage, trans);
		}
		return this;
	}

	public Screen replace(Stage stage) {
		if (LSystem._process != null) {
			LSystem._process.stageSystem.replace(stage);
		}
		return this;
	}

	public Screen replace(Stage stage, StageTransition trans) {
		if (LSystem._process != null) {
			LSystem._process.stageSystem.replace(stage, trans);
		}
		return this;
	}

	public boolean remove(Stage stage) {
		if (LSystem._process != null) {
			LSystem._process.stageSystem.popTo(stage);
		}
		return false;
	}

	public boolean remove(Stage stage, StageTransition trans) {
		if (LSystem._process != null) {
			LSystem._process.stageSystem.remove(stage);
		}
		return false;
	}

	public Screen setAutoDestory(final boolean a) {
		if (desktop != null) {
			desktop.setAutoDestory(a);
		}
		return this;
	}

	public boolean isAutoDestory() {
		if (desktop != null) {
			return desktop.isAutoDestory();
		}
		return false;
	}

	public ResourceLocal getResourceConfig(String path) {
		if (LSystem._base == null) {
			return new ResourceLocal(path);
		}
		return LSystem._base.assets().getJsonResource(path);
	}

	public abstract void resume();

	public abstract void pause();

	public void stop() {
	}// noop

	/**
	 * 释放函数内资源
	 * 
	 */
	public abstract void close();
}

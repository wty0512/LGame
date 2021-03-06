package org.test;

import loon.BaseIO;
import loon.LTexture;
import loon.LTransition;
import loon.Screen;
import loon.canvas.Canvas;
import loon.canvas.Image;
import loon.canvas.LColor;
import loon.event.GameTouch;
import loon.font.LFont;
import loon.opengl.GLEx;
import loon.opengl.LTextureImage;
import loon.stage.ImagePlayer;
import loon.utils.reply.Port;
import loon.utils.timer.LTimerContext;

public class TextureImageTest extends Screen {

	@Override
	public LTransition onTransition() {
		return LTransition.newEmpty();
	}

	@Override
	public void draw(GLEx g) {

	}

	static float width = 100;
	static float height = 100;
	static int offset = 5;
	static String imageSrc = "imagetest1.png";
	static String imageGroundSrc = "imagetest2.png";

	@Override
	public void onLoad() {

		// 设置默认字体大小为20号字
		LFont.setDefaultFont(LFont.getFont(20));
		
		float nwidth = 4 * width, nheight = 4 * height;
		// 创建一个绑定GLEx的Texture图片
		LTextureImage bg = createTextureImage(nwidth, nheight);
		// 直接渲染纹理画面(内部使用framebuffer)
		bg.begin().setColor(LColor.red).fillRect(0, 0, nwidth, nheight)
				.setColor(LColor.blue)
				.fillRect(0, nwidth / 2, nwidth, nheight / 2).end().close();

		// 添加纹理图片到Screen
		add(new ImagePlayer(bg.texture));

		// 加载指定图片，当图片加载成功后
		BaseIO.loadImage(imageSrc).state.onSuccess(new Port<Image>() {
			// 执行如下代码
			public void onEmit(Image image) {

				// 渲染纹理，并作为表演者加入Screen
				LTexture imtex = image.texture();
				getRootPlayer().addAt(new ImagePlayer(imtex), offset, offset);
				getRootPlayer().addAt(new ImagePlayer(imtex), offset,
						offset + 2 * height);

				LTextureImage surf = createTextureImage(image.width(),
						image.height());
				surf.begin().clear().draw(imtex, 0, 0).end().close();
				getRootPlayer().addAt(new ImagePlayer(surf.texture),
						offset + width, offset);
				getRootPlayer().addAt(new ImagePlayer(surf.texture),
						offset + width, offset + 2 * height);

				Canvas canvas = image.getCanvas();
				canvas.draw(image, 0, 0);
				LTexture texture = canvas.toTexture();
				getRootPlayer().addAt(new ImagePlayer(texture),
						offset + 2 * width, offset);
				getRootPlayer().addAt(new ImagePlayer(texture),
						offset + 2 * width, offset + 2 * height);
			}
		});

		// 添加图片并载入Screen
		Image baseGround = BaseIO.loadImage(imageGroundSrc);
		getRootPlayer().addAt(new ImagePlayer(baseGround), 3 * width, 0);
		

		add(MultiScreenTest.getBackButton(this));
	}

	@Override
	public void alter(LTimerContext timer) {

	}

	@Override
	public void resize(int width, int height) {

	}

	@Override
	public void touchDown(GameTouch e) {

	}

	@Override
	public void touchUp(GameTouch e) {

	}

	@Override
	public void touchMove(GameTouch e) {

	}

	@Override
	public void touchDrag(GameTouch e) {

	}

	@Override
	public void resume() {

	}

	@Override
	public void pause() {

	}

	@Override
	public void close() {

	}
}

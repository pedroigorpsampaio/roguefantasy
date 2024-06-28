package com.mygdx.game;

import static com.badlogic.gdx.graphics.Texture.TextureFilter.Linear;
import static com.badlogic.gdx.graphics.Texture.TextureFilter.MipMap;
import static com.badlogic.gdx.graphics.Texture.TextureFilter.MipMapLinearNearest;
import static com.badlogic.gdx.graphics.Texture.TextureFilter.Nearest;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.tools.texturepacker.TexturePacker;

// Please note that on macOS your application needs to be started with the -XstartOnFirstThread JVM argument
public class DesktopLauncher {
	public final static boolean PACK_TEXTURES = true;
	public static void main (String[] arg) {
		if(PACK_TEXTURES) {
			TexturePacker.Settings settings = new TexturePacker.Settings();
			// If your images are numbered, but not for animation, you'll probably need this.
			settings.useIndexes = false;
			settings.filterMin = MipMap;
			settings.filterMag = MipMap;
//			settings.duplicatePadding = false;
//			settings.bleed = true;
//			settings.pot = true;
//			settings.edgePadding = true;
//			settings.paddingY = 2;
//			settings.paddingX = 2;
//			settings.wrapX = Texture.TextureWrap.ClampToEdge;
//			settings.wrapY = Texture.TextureWrap.ClampToEdge;
//			settings.bleedIterations = 256;
			TexturePacker.process(settings, "assets/world/novaterra_raw_textures", "assets/world/novaterra_packed_textures", "novaterra.atlas");
		}
		Lwjgl3ApplicationConfiguration config = new Lwjgl3ApplicationConfiguration();
		//config.setForegroundFPS(60);
		//config.setIdleFPS(60);
		config.setWindowedMode(1280, 720);
		config.setTitle("project-rogue-fantasy");

		//config.useVsync(true);

		new Lwjgl3Application(new RogueFantasy(), config);
	}

}



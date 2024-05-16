package com.mygdx.server.ui;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.InputListener;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.ButtonGroup;
import com.badlogic.gdx.scenes.scene2d.ui.HorizontalGroup;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.ui.TextField;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.ScreenUtils;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import com.esotericsoftware.minlog.Log;
import com.mygdx.server.network.LoginServer;
import com.mygdx.server.ui.CommandDispatcher.CmdReceiver;
import com.mygdx.server.ui.CommandDispatcher.Command;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.LinkedList;
import java.util.ListIterator;

public class RogueFantasyServer extends ApplicationAdapter implements CmdReceiver {
	LoginServer loginServer;
	CommandDispatcher dispatcher;
	SpriteBatch batch;
	private PrintStream archived;
	private ByteArrayOutputStream outputStream;
	private String utf8, data ="";
	private Skin skin;
	private Stage stage;
	private Table uiTable;
	private Label logLabel;
	private ScrollPane logScrollPane;
	private TextButton sendBtn;
	private byte[] lastBaos = {};
	private LinkedList<String> globalLogs, loginLogs, chatLogs, gameLogs; // will contain logs from each server and global logs
	private LogTab currentTab; // current log tab
	private TextButton globalTabBtn, loginTabBtn, chatTablBtn, gameTabBtn; // tab buttons
	private Label titleLabel;
	private TextField cmdField;
	private ButtonGroup<TextButton> bgTabs; // button group containing button tabs

	@Override
	public void create ()  {
		batch = new SpriteBatch();
		skin = new Skin(Gdx.files.internal("skin/uiskin.json"));

		// initialize linked lists of logs
		globalLogs = new LinkedList<>();
		loginLogs = new LinkedList<>();
		chatLogs = new LinkedList<>();
		gameLogs = new LinkedList<>();
		currentTab = LogTab.GLOBAL; // starts at global tab, all info tab

		// redirect system out to use it on server console
		outputStream = new ByteArrayOutputStream();
		utf8 = StandardCharsets.UTF_8.name();
		try {
			archived =  new PrintStream(outputStream, true, utf8);
		} catch (UnsupportedEncodingException e) {
			throw new RuntimeException(e);
		}
		System.setErr(System.out); // err becomes default out
		System.setOut(archived);
		// log level
		Log.set(Log.LEVEL_DEBUG);

		// starts login server
		try {
			loginServer = new LoginServer();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		
		// creates ui stage
		stage = new Stage(new ScreenViewport());
		Gdx.input.setInputProcessor(stage);

		// Title label
		String currentDateTime = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());
		titleLabel = new Label("Server deployed at: " + currentDateTime + " (Server Time)", skin);
		titleLabel.setAlignment(Align.center);

		// log label
		logLabel = new Label(data, skin);
		logLabel.setAlignment(Align.topLeft);
		logLabel.setWrap(true);

		// tab buttons
		globalTabBtn = new TextButton("Global", skin, "toggle");
		loginTabBtn = new TextButton("Login", skin, "toggle");
		chatTablBtn = new TextButton("Chat", skin, "toggle");
		gameTabBtn = new TextButton("Game", skin, "toggle");
		bgTabs = new ButtonGroup<>(globalTabBtn, loginTabBtn, chatTablBtn, gameTabBtn);
		bgTabs.setMinCheckCount(1);
		bgTabs.setMaxCheckCount(1);
		bgTabs.setUncheckLast(true);
		globalTabBtn.setChecked(true); // global is selected at the start
		
		// command input textfield
		cmdField = new TextField("", skin);
		cmdField.setMessageText("enter command...");

		// send command button
		sendBtn = new TextButton("Send", skin);

		// ui table with ui actors
		uiTable = new Table(skin);
		uiTable.setPosition(Gdx.graphics.getWidth() / 2.0f, Gdx.graphics.getHeight() / 2.0f, Align.center);
		uiTable.defaults().spaceBottom(0).padRight(5).padLeft(5).minWidth(320);
		uiTable.add(titleLabel).center().colspan(2).padBottom(10).padTop(10);
		uiTable.row();
		// tab buttons
		HorizontalGroup hgTabBtns = new HorizontalGroup();
		hgTabBtns.addActor(globalTabBtn);
		hgTabBtns.addActor(loginTabBtn);
		hgTabBtns.addActor(chatTablBtn);
		hgTabBtns.addActor(gameTabBtn);
		hgTabBtns.space(4);
		uiTable.add(hgTabBtns).colspan(1).left();
		uiTable.row();
		// scrollable log table
		Table scrollTable = new Table();
		scrollTable.add(logLabel).grow().padTop(5).padLeft(5);
		scrollTable.row();
		scrollTable.pack();
		// uses scrollpane to make table scrollable
		logScrollPane = new ScrollPane(scrollTable, skin);
		logScrollPane.setFadeScrollBars(false);
		logScrollPane.setScrollbarsOnTop(true);
		logScrollPane.setSmoothScrolling(true);
		logScrollPane.setupOverscroll(0,0,0);

		uiTable.add(logScrollPane).colspan(2).size(Gdx.graphics.getWidth()*0.996f, Gdx.graphics.getHeight()*0.8f).center();
		uiTable.row();
		uiTable.add(cmdField).padTop(10).left().colspan(1).grow().height(30);
		uiTable.add(sendBtn).padTop(10).colspan(1).width(152).height(32);
		uiTable.pack();

		stage.addActor(uiTable);

		// ui controller
		new UIController();
		// cmd dispatcher
		dispatcher = new CommandDispatcher(this, loginServer);
	}

	// only scrolls to the end if bar is at the end,
	// so automatic scroll will happen only on new log
	// and when the bar is all the way down
	public void scrollToEnd() {
		if(logScrollPane.isBottomEdge()) {
			logScrollPane.layout();
			logScrollPane.scrollTo(0, 0, 0, 0);
		}
	}

	@Override
	public void render () {
		// if new log occurs, proccess it
		boolean newLog = lastBaos.length != outputStream.size();
		if(newLog) {
			//Log.debug("global", "New log input");

			try {
				data = outputStream.toString(utf8); // gets new data in stream, after last reset
			} catch (UnsupportedEncodingException e) {
				throw new RuntimeException(e);
			}
			parseLogData(data); // parses new log data
			scrollToEnd(); // scrolls to the end if user is already at the end of scrollpane
			// updates last bytearray
			outputStream.reset(); // empty stream to keep it from growing
			lastBaos = outputStream.toByteArray();
		}

		ScreenUtils.clear(0.4f, 0.4f, 0.5f, 1);
//		batch.begin();
//		font.draw(batch, data, 222, 222);
//		batch.end();
		stage.act(Math.min(Gdx.graphics.getDeltaTime(), 1 / 30f));
		stage.draw();

	}

	/**
	 * Parses log data, allocating it to their respective linked lists
	 * @param data	the log data to be parsed in Log (minlog) format from kryonet
	 *              can be multi line strings, since it will be split by lines
	 */
	private void parseLogData(String data) {
		// splits each line of the log data
		String[] splitData = data.split("\\n");
		// for each line allocate it to the respective linked list
		for (int i=0; i<splitData.length; i++) {
			String tag = splitData[i].substring(splitData[i].indexOf("[")+1, splitData[i].indexOf("]"));

			switch(tag) {
				case "login-server":
					storeLog(loginLogs, splitData[i]);
					if(currentTab == LogTab.LOGIN) // updates if is selected tab
						updateLogLabel(loginLogs);
					break;
				case "chat-server":
					storeLog(chatLogs, splitData[i]);
					if(currentTab == LogTab.CHAT) // updates if is selected tab
						updateLogLabel(chatLogs);
					break;
				case "game-server":
					storeLog(gameLogs, splitData[i]);
					if(currentTab == LogTab.GAME) // updates if is selected tab
						updateLogLabel(gameLogs);
					break;
				default:
					break;
			}
			storeLog(globalLogs, splitData[i]); // always adds to global
			if(currentTab == LogTab.GLOBAL) // updates label if selected tab is global
				updateLogLabel(globalLogs);
		}
	}

	/**
	 * Updates the log label text
	 * @param logList	the log list to use for log label
	 */
	private void updateLogLabel(LinkedList<String> logList) {
		// By using String Builder
		StringBuilder stringBuilder = new StringBuilder();

		// using ListIterator for traversing a linked list
		ListIterator<String> listIterator = logList.listIterator();

		while (listIterator.hasNext()) {
			// using append method for appending string
			stringBuilder.append(listIterator.next())
					.append("\n");
		}
		logLabel.setText(stringBuilder);
	}

	/**
	 * Logs to the log list keeping it within the max set size
	 * To do so it keeps only the MAX_SIZE new log inputs
	 * @param logList	the log list that the log will be added
	 * @param log		the new log to be added to the list
	 */
	private void storeLog(LinkedList<String> logList, String log) {
		logList.add(log); // adds to the log list
		if(logList.size() > GlobalConfig.MAX_LOG_DISPLAY_SIZE) // keep new elements only, within max size
			logList.removeFirst();
	}

	@Override
	public void resize (int width, int height) {
		stage.getViewport().update(width, height, true);
		uiTable.setPosition(Gdx.graphics.getWidth() / 2.0f ,Gdx.graphics.getHeight() / 2.0f, Align.center);
		float uiZoomFactor = 720f / Gdx.graphics.getHeight();
		((OrthographicCamera)stage.getCamera()).zoom = uiZoomFactor; // zoom in console
	}
	
	@Override
	public void dispose () {
		loginServer.stop(); // stops server to save it
		batch.dispose();
		archived.close();
		stage.dispose();
		skin.dispose();
		try {
			outputStream.close();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	// process commands received via console
	@Override
	public void process(Command cmd) {
		switch (cmd.type) {
			case CLEAR_LOG:
				clearTab();
				break;
			default:
				break;
		}
	}

	// clears current tab
	private void clearTab() {
		switch(currentTab) {
			case CHAT:
				chatLogs.clear();
				updateLogLabel(chatLogs);
				break;
			case GAME:
				gameLogs.clear();
				updateLogLabel(gameLogs);
				break;
			case LOGIN:
				loginLogs.clear();
				updateLogLabel(loginLogs);
				break;
			case GLOBAL:
				globalLogs.clear();
				updateLogLabel(globalLogs);
				break;
			default:
				System.err.println("Unknown tab selected");
				break;
		}
	}

	/**
	 * A nested class that controls the server ui
	 */
	class UIController {
		// constructor adds listeners to the actors
		public UIController() {
			sendBtn.addListener(new ClickListener() {
				@Override
				public void clicked(InputEvent event, float x, float y) {
					super.clicked(event, x, y);
					sendCmd(cmdField.getText());
				}
			});
			globalTabBtn.addListener(new ChangeListener() {
				@Override
				public void changed(ChangeEvent event, Actor actor) {
					if(globalTabBtn.isChecked()) {
						changeTab(LogTab.GLOBAL, globalLogs);
					}
				}
			});
			loginTabBtn.addListener(new ChangeListener() {
				@Override
				public void changed(ChangeEvent event, Actor actor) {
					if(loginTabBtn.isChecked()) {
						changeTab(LogTab.LOGIN, loginLogs);
					}
				}
			});
			chatTablBtn.addListener(new ChangeListener() {
				@Override
				public void changed(ChangeEvent event, Actor actor) {
					if(chatTablBtn.isChecked()) {
						changeTab(LogTab.CHAT, chatLogs);
					}
				}
			});
			gameTabBtn.addListener(new ChangeListener() {
				@Override
				public void changed(ChangeEvent event, Actor actor) {
					if(gameTabBtn.isChecked()) {
						changeTab(LogTab.GAME, gameLogs);
					}
				}
			});
			cmdField.addListener(new InputListener() {
				@Override
				public boolean keyDown(InputEvent event, int keycode) {
					if (keycode == Input.Keys.ENTER) {
						sendCmd(cmdField.getText());
					}
					return false;
				}
			});
			stage.addListener(new InputListener(){ // just to control gain and lose of keyboard focus by cmd text field and tab tabs
				@Override
				public boolean touchDown(InputEvent event, float x, float y, int pointer, int button) {
					Actor hit = stage.hit(x, y, true);
					if(hit == null || !hit.hasKeyboardFocus())
						stage.setKeyboardFocus(null);
					return super.touchDown(event, x, y, pointer, button);
				}

				@Override
				public boolean keyDown(InputEvent event, int keycode) {
					if (keycode == Input.Keys.ENTER) {
						if(!cmdField.hasKeyboardFocus())
							stage.setKeyboardFocus(cmdField);
					} else if (keycode == Input.Keys.ESCAPE) {
						if(cmdField.hasKeyboardFocus())
							stage.setKeyboardFocus(null);
					} else if (keycode == Input.Keys.TAB) {
						int nextIdx = (bgTabs.getCheckedIndex() + 1) % bgTabs.getButtons().size;
						TextButton next = bgTabs.getButtons().get(nextIdx);
						next.setChecked(true);
					}
					return false;
				}
			});
		}

		// sends commands to the parser and executor
		private void sendCmd(String cmd) {
			if(cmd == "") return; // returns if there is no cmd
			cmdField.setText(""); // clear cmd field
			// sends command to dispatcher that will deal with parsing and processing
			dispatcher.processCommand(cmd);
		}

		// change selected log tab and updates label text
		private void changeTab(LogTab tab, LinkedList<String> log) {
			currentTab = tab;
			updateLogLabel(log);
			logScrollPane.layout();
			logScrollPane.setScrollPercentY(100);
			logScrollPane.updateVisualScroll();
		}
	}

	class GlobalConfig {
		static final int MAX_LOG_DISPLAY_SIZE = 300;
	}

	enum LogTab {
		GLOBAL,
		LOGIN,
		GAME,
		CHAT
	}

}

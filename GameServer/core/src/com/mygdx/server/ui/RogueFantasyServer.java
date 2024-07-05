package com.mygdx.server.ui;

import static com.esotericsoftware.minlog.Log.LEVEL_DEBUG;
import static com.esotericsoftware.minlog.Log.LEVEL_ERROR;
import static com.esotericsoftware.minlog.Log.LEVEL_INFO;
import static com.esotericsoftware.minlog.Log.LEVEL_TRACE;
import static com.esotericsoftware.minlog.Log.LEVEL_WARN;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.audio.Music;
import com.badlogic.gdx.graphics.Color;
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
import com.badlogic.gdx.utils.Timer;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import com.esotericsoftware.minlog.Log;
import com.mygdx.server.entity.WorldMap;
import com.mygdx.server.network.GameServer;
import com.mygdx.server.network.LoginServer;
import com.mygdx.server.ui.CommandDispatcher.CmdReceiver;
import com.mygdx.server.ui.CommandDispatcher.Command;
import com.mygdx.server.util.Common;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.management.ManagementFactory;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class RogueFantasyServer extends ApplicationAdapter implements CmdReceiver {
	// game servers
	LoginServer loginServer;
	GameServer gameServer;
	// command dispatcher
	CommandDispatcher dispatcher;
	// ui vars
	SpriteBatch batch;
	private Skin skin;
	private static Stage stage;
	private Table uiTable;
	private Label logLabel;
	private ScrollPane logScrollPane;
	private TextButton sendBtn;
	private ArrayList<String> cmdHistory; // command history to be saved
	private int cmdOffset = -1; // cmd history offset, when user scrolls through cmd history
	private List<String> globalLogs, loginLogs, chatLogs, gameLogs; // will contain logs from each server and global logs
	private ServerChannel currentTab; // current log tab
	private TextButton globalTabBtn, loginTabBtn, chatTablBtn, gameTabBtn, metricsBtn; // tab buttons
	private Label titleLabel;
	private TextField cmdField;
	private ButtonGroup<TextButton> bgTabs; // button group containing button tabs
	private final long firstServerTime = System.currentTimeMillis(); // time when server started
	private UIController controller;
	private boolean currentTabNeedsUpdate = false;
	private double cpuLoad;
	private long ramLoad;
	private Music timeCommandoSong, corneriaSong, demonBlueSong; // songs for gimmick purposes
	private HashMap<String, Music> musics; // map of available musics accessible via file name as key
	public static WorldMap world;
	private static boolean isWorldVisible = false;
	public static String worldStateMessageSize = "0";

	@Override
	public void create ()  {
		batch = new SpriteBatch();
		skin = new Skin(Gdx.files.internal("skin/uiskin.json"));
		Label.LabelStyle lStyle = skin.get(Label.LabelStyle.class);
		//lStyle.fontColor = Color.RED;
		lStyle.font.getData().markupEnabled = true;
		skin.add("newLabelStyle", lStyle, Label.LabelStyle.class);

		// load musics into map of available musics
		loadMusics();

		// initialize linked lists of logs
		globalLogs = Collections.synchronizedList(new ArrayList<>());
		loginLogs = Collections.synchronizedList(new ArrayList<>());
		chatLogs = Collections.synchronizedList(new ArrayList<>());
		gameLogs = Collections.synchronizedList(new ArrayList<>());
		cmdHistory = new ArrayList<>();
		currentTab = ServerChannel.GLOBAL; // starts at global tab, all info tab

		Log.setLogger(new ServerLogger()); // sets logger class to be able to catch logs and display it on screen

		Log.set(LEVEL_DEBUG); // sets log level

		// creates ui stage
		stage = new Stage(new ScreenViewport());
		Gdx.input.setInputProcessor(stage);

		// Title label
		String currentDateTime = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());
		titleLabel = new Label("Server deployed at: " + currentDateTime + " (Server Time)", skin);
		titleLabel.setAlignment(Align.center);

		// log label
		logLabel = new Label("", skin, "newLabelStyle");
		logLabel.setAlignment(Align.topLeft);
		logLabel.setWrap(true);

		// tab buttons
		globalTabBtn = new TextButton("Global", skin, "toggle");
		loginTabBtn = new TextButton("Login", skin, "toggle");
		chatTablBtn = new TextButton("Chat", skin, "toggle");
		gameTabBtn = new TextButton("Game", skin, "toggle");
		metricsBtn = new TextButton("Metrics", skin, "toggle");
		bgTabs = new ButtonGroup<>(globalTabBtn, loginTabBtn, chatTablBtn, gameTabBtn, metricsBtn);
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
		hgTabBtns.addActor(metricsBtn);
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
		controller = new UIController();

		// starts update timer, that updates UI metrics between set intervals
		Timer.schedule(controller.updateTimer, 0f, GlobalConfig.METRICS_UPDATE_INTERVAL);
		// cmd dispatcher
		dispatcher = new CommandDispatcher(this, loginServer, gameServer);
		// load map
		world = WorldMap.getInstance();
		world.init("world/novaterra.tmx", batch);
		// starts all servers
		startServer(ServerChannel.ALL);
	}

	// loads existing song files into map of available songs to play
	private void loadMusics() {
		musics = new HashMap<>();
		ArrayList<String> songFileNames = Common.getListOfSongs();
		for(String song : songFileNames) {
			musics.put(song, Gdx.audio.newMusic(Gdx.files.internal("music/"+song)));
		}

		// gimmick musics (use play command)
//		timeCommandoSong = Gdx.audio.newMusic(Gdx.files.internal("music/time_commando.mp3"));
//		corneriaSong = Gdx.audio.newMusic(Gdx.files.internal("music/corneria.mp3"));
//		demonBlueSong = Gdx.audio.newMusic(Gdx.files.internal("music/demon_blue.mp3"));
	}

	private void startServer(ServerChannel serverType) {
		if(serverType == ServerChannel.ALL || serverType == ServerChannel.LOGIN) {
			// starts login server if not online already
			if(loginServer == null || !loginServer.isOnline()) {
				Log.info("cmd", "Starting " +serverType.text+ " server...");
				loginServer = LoginServer.getInstance(); // get login server instance
				loginServer.connect(); // connects/starts login server
				dispatcher.setLoginServer(loginServer);  // sets login server for the command dispatcher
			} else {
				Log.info("cmd", "Login server is already running!");
			}
		}
		if(serverType == ServerChannel.ALL || serverType == ServerChannel.CHAT) {
			// TODO chat server
		}
		if(serverType == ServerChannel.ALL || serverType == ServerChannel.GAME) {
			// TODO game server
			// starts login server if not online already
			if(gameServer == null || !gameServer.isOnline()) {
				Log.info("cmd", "Starting " +serverType.text+ " server...");
				gameServer = GameServer.getInstance(); // get game server instance
				gameServer.connect();  // connects/starts game server
				dispatcher.setGameServer(gameServer); // sets game server for the command dispatcher
			} else
				Log.info("cmd", "Game server is already running!");
		}
	}

	// just null servers references here
	private void stopServer(ServerChannel serverType) {
		if(serverType == ServerChannel.ALL || serverType == ServerChannel.LOGIN) {loginServer = null;}
		if(serverType == ServerChannel.ALL || serverType == ServerChannel.CHAT) {
			// TODO chat server
		}
		if(serverType == ServerChannel.ALL || serverType == ServerChannel.GAME) {gameServer = null;}
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

		if(currentTabNeedsUpdate) {
			updateLogLabel(currentTab);
			scrollToEnd(); // scroll to keep up with log (only scrolls if bar is already bottom)
			currentTabNeedsUpdate = false;
		}

		ScreenUtils.clear(0.4f, 0.4f, 0.5f, 1);
		stage.act(Math.min(Gdx.graphics.getDeltaTime(), 1 / 30f));
		stage.draw();

		if(isWorldVisible) { // render map for debug
			//batch.setColor(new Color(Color.RED));
			world.render();
		}

	}

	/**
	 * Returns the log list that corresponds to the tab received in parameter
	 * @param tab	the tab to get the log list from
	 * @return		the log list that corresponds to the tab
	 */
	private List<String> logListFromTab(ServerChannel tab) {
		List<String> list;

		switch (tab) {
			case LOGIN:
				list = loginLogs;
				break;
			case CHAT:
				list = chatLogs;
				break;
			case GAME:
				list = gameLogs;
				break;
			default:
				list = globalLogs;
				break;
		}

		return list;
	}

	/**
	 * Updates the log label text
	 * @param tab	the tab to update log list
	 */
	private void updateLogLabel(ServerChannel tab) {
		if(tab != ServerChannel.METRICS) { // if its not metrics, load logs
			List<String> list = logListFromTab(tab); // gets log from tab
			StringBuilder stringBuilder = new StringBuilder(); // str builder that will build log text
			// log list may be accessed concurrently by server when receiving messages from clients
			// since logger implementation access directly log lists to add new logs
			synchronized(list) {
				Iterator i = list.iterator(); // Must be in synchronized block
				while (i.hasNext())
					stringBuilder.append(i.next()).append("\n");
			}

			logLabel.setText(stringBuilder);
		}
		else { // if it is metrics tab, load serve metrics into label
			controller.updateMetrics();
		}
	}

	/**
	 * Logs to the log list keeping it within the max set size
	 * To do so it keeps only the MAX_SIZE new log inputs
	 * @param logList	the log list that the log will be added
	 * @param log		the new log to be added to the list
	 */
	private void storeLog(List<String> logList, String log) {
		logList.add(log); // adds to the log list
		if(logList.size() > GlobalConfig.MAX_LOG_DISPLAY_SIZE) // keep new elements only, within max size
			logList.remove(0);
	}

	/**
	 * Logs to the log list keeping it within the max set size
	 * To do so it keeps only the MAX_SIZE new log inputs
	 * @param tab	the current tab that the log will be added
	 * @param log	the new log to be added to the list
	 */
	private void storeLog(ServerChannel tab, String log) {
		// if not a log tab return
		if(tab != ServerChannel.GLOBAL && tab != ServerChannel.LOGIN &&
				tab != ServerChannel.CHAT && tab != ServerChannel.GAME)
			return;
		List<String> list = logListFromTab(tab);
		list.add(log); // adds to the log list
		if(list.size() > GlobalConfig.MAX_LOG_DISPLAY_SIZE) // keep new elements only, within max size
			list.remove(0);
	}

	@Override
	public void resize (int width, int height) {
		stage.getViewport().update(width, height, true);
		world.resize(width, height);
		uiTable.setPosition(Gdx.graphics.getWidth() / 2.0f ,Gdx.graphics.getHeight() / 2.0f, Align.center);
		float uiZoomFactor = 720f / Gdx.graphics.getHeight();
		((OrthographicCamera)stage.getCamera()).zoom = uiZoomFactor; // zoom in console
	}

	@Override
	public void dispose () {
		loginServer.stop(); // stops server to save it
		gameServer.stop();
		batch.dispose();
		stage.dispose();
		skin.dispose();
		world.dispose();

		System.exit(0);
		Gdx.app.exit();
	}

	// closes all servers saving changes before ending process
	public void safeExit() {
		Log.info("cmd", "Closing all servers and exiting...");

		loginServer.stop();
		gameServer.stop();

		// waits until both servers are saved and closed before finishing process
		while(loginServer.isOnline() || gameServer.isOnline());

		new Thread(new Runnable() {
			@Override
			public void run() {
				long time = System.currentTimeMillis();
				while (System.currentTimeMillis() < time + 2000){}
				Gdx.app.postRunnable(new Runnable() {
					@Override
					public void run() {
						//Gdx.app.exit();
						System.exit(0);
					}
				});
			}
		}).start();
	}

	public static void hideMap() {
		Gdx.input.setInputProcessor(stage);
		isWorldVisible = false;
	}

	// process commands received via dispatcher
	@Override
	public void process(Command cmd) {
		RogueFantasyServer.ServerChannel receiver;
		switch (cmd.type) {
			case CLEAR_LOG:
				try {
					controller.clearTab(ServerChannel.fromString(cmd.args[0]));
				} catch (Exception e) { // argument does not match with possible ones
					Log.info("cmd", "Invalid argument: "+cmd.args[0]);
				}
				break;
			case START:
			case RESTART:
				receiver = dispatcher.getReceiver(cmd);
				startServer(receiver);
				break;
			case STOP:
				receiver = dispatcher.getReceiver(cmd);
				stopServer(receiver);
				break;
			case PLAY:
				controller.playSong(cmd.args[0], true); // plays song exclusively
				break;
			case PAUSE:
				controller.stopSongs(); // stops any song that is currently playing
				break;
			case EXIT:
				safeExit(); // closes all servers saving changes before closing exe
				break;
			default:
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
						changeTab(ServerChannel.GLOBAL);
					}
				}
			});
			loginTabBtn.addListener(new ChangeListener() {
				@Override
				public void changed(ChangeEvent event, Actor actor) {
					if(loginTabBtn.isChecked()) {
						changeTab(ServerChannel.LOGIN);
					}
				}
			});
			chatTablBtn.addListener(new ChangeListener() {
				@Override
				public void changed(ChangeEvent event, Actor actor) {
					if(chatTablBtn.isChecked()) {
						changeTab(ServerChannel.CHAT);
					}
				}
			});
			gameTabBtn.addListener(new ChangeListener() {
				@Override
				public void changed(ChangeEvent event, Actor actor) {
					if(gameTabBtn.isChecked()) {
						changeTab(ServerChannel.GAME);
					}
				}
			});
			metricsBtn.addListener(new ChangeListener() {
				@Override
				public void changed(ChangeEvent event, Actor actor) {
					if(metricsBtn.isChecked()) {
						changeTab(ServerChannel.METRICS);
					}
				}
			});
			cmdField.addListener(new InputListener() {
				@Override
				public boolean keyDown(InputEvent event, int keycode) {
					if (keycode == Input.Keys.ENTER || keycode == Input.Keys.NUMPAD_ENTER) {
						sendCmd(cmdField.getText());
						cmdOffset = -1; // reset command offset if new input is made
					} else if (keycode == Input.Keys.UP) {
						if(!downHistoryTimer.isScheduled())
							Timer.schedule(upHistoryTimer, 0f, .2f);
					} else if (keycode == Input.Keys.DOWN) {
						if(!upHistoryTimer.isScheduled())
							Timer.schedule(downHistoryTimer, 0f, .2f);
					}
					return false;
				}

				@Override
				public boolean keyUp(InputEvent event, int keycode) {
					if (keycode == Input.Keys.UP) {
						upHistoryTimer.cancel();
					} else if (keycode == Input.Keys.DOWN) {
						downHistoryTimer.cancel();
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
					if (keycode == Input.Keys.ENTER || keycode == Input.Keys.NUMPAD_ENTER) {
						if(!cmdField.hasKeyboardFocus())
							stage.setKeyboardFocus(cmdField);
					} else if (keycode == Input.Keys.ESCAPE) {
						if(cmdField.hasKeyboardFocus())
							stage.setKeyboardFocus(null);
					} else if (keycode == Input.Keys.TAB) {
						int nextIdx;
						if(!Gdx.input.isKeyPressed(Input.Keys.SHIFT_LEFT))
							nextIdx = (bgTabs.getCheckedIndex() + 1) % bgTabs.getButtons().size;
						else
							nextIdx = (bgTabs.getCheckedIndex() + bgTabs.getButtons().size - 1) % bgTabs.getButtons().size;
						TextButton next = bgTabs.getButtons().get(nextIdx);
						next.setChecked(true);
					} else if(keycode == Input.Keys.M && Gdx.input.isKeyPressed(Input.Keys.SHIFT_LEFT)) {
						isWorldVisible = isWorldVisible ? false : true;
						if(isWorldVisible) Gdx.input.setInputProcessor(world);
						else Gdx.input.setInputProcessor(stage);
					}
					return false;
				}
			});
		}
		// timer to go through cmd history upwards when up arrow is pressed down
		private Timer.Task upHistoryTimer = new Timer.Task() {
			@Override
			public void run() {
				cmdOffset++;
				if(cmdOffset>=cmdHistory.size()) { // already at last command
					cmdOffset = cmdHistory.size() - 1;
				}
				// update text field retrieving cmd from history
				cmdField.setText(retrieveCmd());
				cmdField.setCursorPosition(cmdField.getText().length());
			}
		};
		// timer to go through cmd history downwards when down arrow is pressed down
		private Timer.Task downHistoryTimer = new Timer.Task() {
			@Override
			public void run() {
				cmdOffset--;
				if(cmdOffset < 0)  { // already at most recent command
					cmdOffset = 0;
				}
				// update text field retrieving cmd from history
				cmdField.setText(retrieveCmd());
				cmdField.setCursorPosition(cmdField.getText().length());
			}
		};

		// timer that updates metrics on intervals
		private Timer.Task updateTimer = new Timer.Task() {
			@Override
			public void run() {
				cpuLoad = ManagementFactory.getPlatformMXBean(
						com.sun.management.OperatingSystemMXBean.class).getProcessCpuLoad();
				int mb = 1024 * 1024;
				// get Runtime instance
				Runtime instance = Runtime.getRuntime();
				ramLoad = (instance.totalMemory() - instance.freeMemory()) / mb;
				updateMetrics(); // update metrics tab
			}
		};

		// updates metrics table (only if it is selected)
		private void updateMetrics() {
			if(currentTab == ServerChannel.METRICS) {
				long time = System.currentTimeMillis() - firstServerTime;
				int seconds = (int) (time / 1000) % 60 ;
				int minutes = (int) ((time / (1000*60)) % 60);
				int hours   = (int) ((time / (1000*60*60)) % 24);
				boolean loginIsOn = loginServer != null && loginServer.isOnline();
				StringBuilder b = new StringBuilder();
				b.append("Time elapsed since server was deployed: ");
				if (hours <= 9) b.append('0'); // avoid using format methods to decrease work
				b.append(hours);
				b.append(":");
				if (minutes <= 9) b.append('0');
				b.append(minutes);
				b.append(":");
				if (seconds <= 9) b.append('0');
				b.append(seconds);
				b.append(" (hh:mm:ss)");
				b.append("\n\n");
				b.append("Login Server: ");
				if(loginIsOn) b.append("online");
				else b.append("offline");
				b.append("\n");
				b.append("Chat Server: ");
				//if(loginServer.isOnline()) b.append("online");
				//else b.append("offline");
				b.append("\n");
				b.append("Game Server: ");
				if(gameServer != null && gameServer.isOnline()) b.append("online");
				else b.append("offline");
				b.append("\n\n");
				b.append("Players online: ");
				if(loginIsOn) b.append(gameServer.getNumberOfPlayersOnline());
				b.append("\n");
				b.append("CPU Usage: ");
				b.append((int)(cpuLoad*1000f));
				b.append("%");
				b.append("\n");
				b.append("RAM Usage: ");
				b.append(ramLoad);
				b.append(" MB");
				b.append("\n\n");
				b.append("Global log size: ");
				b.append(globalLogs.size());
				b.append("\n");
				b.append("Login log size: ");
				b.append(loginLogs.size());
				b.append("\n");
				b.append("Chat log size: ");
				b.append(chatLogs.size());
				b.append("\n");
				b.append("Game log size: ");
				b.append(gameLogs.size());
				b.append("\n");
				logLabel.setText(b);
			}
		}

		// retrieves command from command history based on current offset
		private String retrieveCmd() {
			if(cmdHistory == null || cmdHistory.isEmpty()) return ""; // if cmd history is null, there is nothing to retrieve

			// makes sure we are within array bounds
			int idx = cmdHistory.size() - 1 - cmdOffset;
			if(idx < 0) idx = 0;
			if(idx >= cmdHistory.size() - 1) idx = cmdHistory.size() - 1;

			return cmdHistory.get(idx);
		}

		// sends commands to the parser and executor
		private void sendCmd(String cmd) {
			if(cmd == "") return; // returns if there is no cmd
			cmdField.setText(""); // clear cmd field
			// sends command to dispatcher that will deal with parsing and processing
			dispatcher.processCommand(cmd);
			// adds to the linked list of commands history
			cmdHistory.add(cmd); // adds to the log list
			if(cmdHistory.size() > GlobalConfig.MAX_CMD_HISTORY_SIZE) // keep new elements only, within max size
				cmdHistory.remove(0);
		}

		// change selected log tab and updates label text
		private void changeTab(ServerChannel tab) {
			currentTab = tab;
			updateLogLabel(tab);
			logScrollPane.layout();
			logScrollPane.setScrollPercentY(100);
			logScrollPane.updateVisualScroll();
		}

		// clears tab passed via parameter
		private void clearTab(ServerChannel tab) {
			switch(tab) {
				case CHAT:
					chatLogs.clear();
					Log.info("cmd", "Chat log cleared");
					break;
				case GAME:
					gameLogs.clear();
					Log.info("cmd", "Game log cleared");
					break;
				case LOGIN:
					loginLogs.clear();
					Log.info("cmd", "Login log cleared");
					break;
				case GLOBAL:
					globalLogs.clear();
					Log.info("cmd", "Global log cleared");
					break;
				case ALL: // clears all tabs
					chatLogs.clear(); gameLogs.clear(); loginLogs.clear(); globalLogs.clear();
					Log.info("cmd", "All logs cleared");
					break;
				default:
					System.err.println("Unknown tab selected");
					break;
			}
			if(tab.equals(currentTab))
				updateLogLabel(tab);
		}

		// plays song from loaded musics
		private void playSong(String name, boolean exclusive) {
			String fileName = name+".mp3";

			if(musics.get(fileName) == null) {
				Log.info("cmd", "Could not find music with this name: "+name);
				return;
			}

			if(exclusive)
				stopSongs();

			musics.get(fileName).play();
			Log.info("cmd", "Now playing: "+name);
		}

		// stops songs being played
		private void stopSongs() {
			Iterator<Map.Entry<String, Music>> iterator = musics.entrySet().iterator();

			while (iterator.hasNext()) {
				Map.Entry<String, Music> entry = iterator.next();
				entry.getValue().stop();
			}
		}
	}

	// extends logger class and override print method
	// to catch when new log occurs and process it
	// and also to change formatting
	public class ServerLogger extends Log.Logger {
		@Override
		public void log(int level, String category, String message, Throwable ex) {
			StringBuilder builder = new StringBuilder();
			Instant time = Instant.now();
			LocalDateTime ldt = LocalDateTime.ofInstant(time, ZoneId.systemDefault());
			int hours = ldt.getHour();
			int minutes = ldt.getMinute();
			int seconds = ldt.getSecond();
//			int minutes = (int) ((time / (1000*60)) % 60);
//			int hours   = (int) ((time / (1000*60*60)) % 24);
			if (hours <= 9) builder.append('0'); // avoid using format methods to decrease work
			builder.append(hours);
			builder.append(":");
			if (minutes <= 9) builder.append('0');
			builder.append(minutes);
			builder.append(":");
			if (seconds <= 9) builder.append('0');
			builder.append(seconds);
			switch (level) {
				case LEVEL_ERROR:
					builder.append(" [#A32D1D]ERROR: ");
					break;
				case LEVEL_WARN:
					builder.append(" [YELLOW]WARN: ");
					break;
				case LEVEL_INFO:
					builder.append(" [#8CFF00]INFO: ");
					break;
				case LEVEL_DEBUG:
					builder.append(" DEBUG: ");
					break;
				case LEVEL_TRACE:
					builder.append(" [ORANGE]TRACE: ");
					break;
			}
			builder.append('[');
			builder.append(category);
			builder.append("] ");
			builder.append(message);
			if (ex != null) {
				StringWriter writer = new StringWriter();
				ex.printStackTrace(new PrintWriter(writer));
				builder.append('\n');
				builder.append(writer.toString().trim());
			}
			builder.append("[]"); // ends color markup

			String logStr = builder.toString();

			switch(category) {
				case "login-server":
					storeLog(loginLogs, logStr);
					if(currentTab == ServerChannel.LOGIN) // updates if is selected tab
						currentTabNeedsUpdate = true;
					break;
				case "chat-server":
					storeLog(chatLogs, logStr);
					if(currentTab == ServerChannel.CHAT) // updates if is selected tab
						currentTabNeedsUpdate = true;
					break;
				case "game-server":
					storeLog(gameLogs, logStr);
					if(currentTab == ServerChannel.GAME) // updates if is selected tab
						currentTabNeedsUpdate = true;
					break;
				case "cmd": // add cmd to selected tab (if not global, that already adds all tags)
					if(currentTab != ServerChannel.GLOBAL) {
						storeLog(currentTab, logStr);
						currentTabNeedsUpdate = true;
					}
					break;
				default:
					break;
			}
			storeLog(globalLogs, logStr); // always adds to global
			if(currentTab == ServerChannel.GLOBAL) // updates label if selected tab is global
				currentTabNeedsUpdate = true;

		}
	}

	public enum ServerChannel {
		GLOBAL("global"),
		LOGIN("login"),
		GAME("game"),
		CHAT("chat"),
		ALL("all"),
		METRICS("metrics"),
		UNKNOWN("unknown");

		private String text;

		ServerChannel(String text) {
			this.text = text;
		}

		public String getText() {
			return this.text;
		}

		public static ServerChannel fromString(String text) throws Exception{
			for (ServerChannel t : ServerChannel.values()) {
				if (t.text.equalsIgnoreCase(text)) {
					return t;
				}
			}
			throw new Exception("No enum constant with text " + text + " found");
		}
	}

	class GlobalConfig {
		public static final int MAX_CMD_HISTORY_SIZE = 50;
		public static final int MAX_LOG_DISPLAY_SIZE = 300;
		public static final float METRICS_UPDATE_INTERVAL = 0.25f;
	}
}

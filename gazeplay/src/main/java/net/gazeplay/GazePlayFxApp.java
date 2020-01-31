package net.gazeplay;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Dimension2D;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import net.gazeplay.cli.GameSelectionOptions;
import net.gazeplay.cli.ReusableOptions;
import net.gazeplay.cli.UserSelectionOptions;
import net.gazeplay.commons.configuration.ActiveConfigurationContext;
import net.gazeplay.commons.configuration.Configuration;
import net.gazeplay.commons.ui.Translator;
import net.gazeplay.components.CssUtil;
import net.gazeplay.gameslocator.GamesLocator;
import net.gazeplay.latestnews.LatestNewPopup;
import net.gazeplay.ui.scenes.gamemenu.GameMenuController;
import org.springframework.context.ApplicationContext;

import java.awt.*;
import java.awt.event.InputEvent;
import java.util.List;
import java.util.Random;

@Slf4j
public class GazePlayFxApp extends Application {

    @Setter
    private static ApplicationContext applicationContext;

    @Setter
    private static ReusableOptions applicationOptions;

    private ApplicationContext context;

    private ReusableOptions options;

    private GazePlay gazePlay;

    private GameMenuController gameMenuController;

    private GamesLocator gamesLocator;

    private Translator translator;

    @Override
    public void init() {
        this.context = applicationContext;
        this.options = applicationOptions;
        //
        log.info("options = {}", options);
        //
        gazePlay = context.getBean(GazePlay.class);
        gameMenuController = context.getBean(GameMenuController.class);
        gamesLocator = context.getBean(GamesLocator.class);
        translator = context.getBean(Translator.class);
    }

    @Override
    public void start(Stage primaryStage) {
        autosize(primaryStage);
        boolean showUserSelectPage = true;
        if (options != null) {
            final UserSelectionOptions userSelectionOptions = options.getUserSelectionOptions();
            if (userSelectionOptions != null) {
                if (userSelectionOptions.getUserid() != null) {
                    showUserSelectPage = false;
                    ActiveConfigurationContext.switchToUser(userSelectionOptions.getUserid());
                }
                if (userSelectionOptions.isDefaultUser()) {
                    showUserSelectPage = false;
                    ActiveConfigurationContext.switchToDefaultUser();
                }
            }
        }
        log.info("showUserSelectPage = {}", showUserSelectPage);

        final Scene primaryScene = createPrimaryScene(primaryStage);
        configureKeysHandler(primaryScene);

        configurePrimaryStage(primaryStage);

        Configuration config = ActiveConfigurationContext.getInstance();

        LatestNewPopup.displayIfNeeded(config, gazePlay.getTranslator(), gazePlay.getCurrentScreenDimensionSupplier());

        gazePlay.setPrimaryScene(primaryScene);
        gazePlay.setPrimaryStage(primaryStage);

        if (showUserSelectPage) {
            gazePlay.goToUserPage();
        } else {
            log.info("options = {}", options);
            final GameSelectionOptions gameSelectionOptions = options.getGameSelectionOptions();
            if (gameSelectionOptions != null) {
                List<GameSpec> gameSpecs = gamesLocator.listGames(translator);
                String selectedGameNameCode = gameSelectionOptions.getGameNameCode();
                if (selectedGameNameCode == null) {
                    if (gameSelectionOptions.isRandomGame()) {
                        Random random = new Random();
                        int randomGameIndex = random.nextInt(gameSpecs.size());
                        GameSpec selectedGameSpec = gameSpecs.get(randomGameIndex);
                        selectedGameNameCode = selectedGameSpec.getGameSummary().getNameCode();
                    }
                }
                if (selectedGameNameCode != null) {
                    final String searchGameNameCode = selectedGameNameCode;
                    GameSpec selectedGameSpec = gameSpecs.stream()
                        .filter(gameSpec -> gameSpec.getGameSummary().getNameCode().equals(searchGameNameCode))
                        .findFirst()
                        .orElseThrow(() -> new IllegalArgumentException(searchGameNameCode));

                    log.info("gameSpecs = {}", gameSpecs);
                    gameMenuController.onGameSelection(gazePlay, gazePlay.getPrimaryScene().getRoot(), selectedGameSpec, selectedGameSpec.getGameSummary().getNameCode());
                } else {
                    gazePlay.onReturnToMenu();
                }
            } else {
                gazePlay.onReturnToMenu();
            }
        }

        CssUtil.setPreferredStylesheets(ActiveConfigurationContext.getInstance(), gazePlay.getPrimaryScene());
        primaryStage.centerOnScreen();
        primaryStage.show();
    }

    private Scene createPrimaryScene(Stage primaryStage) {
        Pane rootPane = new Pane();
        Scene primaryScene = new Scene(rootPane, primaryStage.getWidth(), primaryStage.getHeight(), Color.BLACK);
        CssUtil.setPreferredStylesheets(ActiveConfigurationContext.getInstance(), primaryScene);
        primaryStage.setScene(primaryScene);
        return primaryScene;
    }

    private void configurePrimaryStage(Stage primaryStage) {
        primaryStage.setTitle("GazePlay");
        primaryStage.setOnCloseRequest((WindowEvent we) -> primaryStage.close());

        String iconImagePath = "data/common/images/gazeplayicon.png";
        Image icon = new Image(iconImagePath);
        primaryStage.getIcons().add(icon);
    }

    private void autosize(Stage primaryStage) {
        Dimension2D screenDimension = gazePlay.getCurrentScreenDimensionSupplier().get();
        //
        primaryStage.setWidth(screenDimension.getWidth() * 0.95);
        primaryStage.setHeight(screenDimension.getHeight() * 0.90);
        primaryStage.setMaximized(false);

        primaryStage.setFullScreen(true);
    }

    private void configureKeysHandler(Scene primaryScene) {
        primaryScene.addEventFilter(KeyEvent.KEY_PRESSED, ke -> {
            if (ke.getCode() == KeyCode.SPACE && ActiveConfigurationContext.getInstance().isGazeMouseEnable()) {
                Platform.runLater(() -> {
                    try {
                        Robot robot = new Robot();
                        robot.mousePress(InputEvent.BUTTON1_MASK);
                        robot.mouseRelease(InputEvent.BUTTON1_MASK);
                    } catch (AWTException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }
                });
            }
        });
        primaryScene.addEventFilter(KeyEvent.KEY_PRESSED, ke -> {
            final Configuration activeConfig = ActiveConfigurationContext.getInstance();
            if (ke.getCode() == KeyCode.S && activeConfig.isGazeMouseEnable()) {
                activeConfig.setMouseFree(!activeConfig.isMouseFree());
            }
        });
    }

}

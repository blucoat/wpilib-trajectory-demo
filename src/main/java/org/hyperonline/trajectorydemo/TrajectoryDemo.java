package org.hyperonline.trajectorydemo;

import java.io.FileReader;
import java.io.Reader;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVRecord;

import edu.wpi.first.wpilibj.geometry.Pose2d;
import edu.wpi.first.wpilibj.geometry.Rotation2d;
import edu.wpi.first.wpilibj.geometry.Translation2d;
import edu.wpi.first.wpilibj.trajectory.Trajectory;
import edu.wpi.first.wpilibj.trajectory.TrajectoryConfig;
import edu.wpi.first.wpilibj.trajectory.TrajectoryGenerator;
import edu.wpi.first.wpilibj.util.Units;
import javafx.application.Application;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.paint.Color;
import javafx.scene.shape.LineTo;
import javafx.scene.shape.MoveTo;
import javafx.scene.shape.Path;
import javafx.scene.shape.PathElement;
import javafx.stage.Stage;

public class TrajectoryDemo extends Application {

	// Pixels per meter, used for drawing paths on the screen.
	private static final double PIXELS_PER_METER = 100;
	
	// Height of the feild, in meters
	private static final double FIELD_HEIGHT = Units.inchesToMeters(180);
	private static final double FIELD_WIDTH = Units.inchesToMeters(360);
	
	// Height of the window, in pixels
	private static final double WINDOW_HEIGHT = FIELD_HEIGHT * PIXELS_PER_METER;
	private static final double WINDOW_WIDTH = FIELD_WIDTH * PIXELS_PER_METER;

	@Override
	public void start(Stage primaryStage) throws Exception {
		// Read a list of points from a CSV file.
		Reader in = new FileReader("slalompath_nobom.csv");
		List<Translation2d> waypoints = CSVFormat.DEFAULT.parse(in).getRecords().stream()
				.map(TrajectoryDemo::recordToTranslation2d)   // convert CSV records to Translation2d's
				.map(t -> t.times(Units.inchesToMeters(1.0))) // scale so units are in meters
				.collect(Collectors.toList());                // collect results into a list

		if (waypoints.size() < 2) {
			throw new RuntimeException("Less than two waypoints provided");
		}

		// Create a smooth trajectory from the list of waypoints
		Pose2d start = new Pose2d(waypoints.get(0), new Rotation2d(0));
		Pose2d end = new Pose2d(waypoints.get(waypoints.size() - 1), new Rotation2d(Math.PI));
		List<Translation2d> interior = waypoints.subList(1, waypoints.size() - 1);
		TrajectoryConfig config = new TrajectoryConfig(4, 4);
		Trajectory trajectory = TrajectoryGenerator.generateTrajectory(start, interior, end, config);

		// Get a list of points in the new trajecotry
		List<Translation2d> trajectoryPoints = trajectory.getStates().stream()
				.map(state -> state.poseMeters.getTranslation())
				.collect(Collectors.toList());

		// Plot both the original waypoints and the new trajectory as paths.
		// Path is part of JavaFX.  It refers to a path drawn on the screen.
		Path origPath = makePath(waypoints);
		origPath.setStroke(Color.BLUE);
		Path smoothPath = makePath(trajectoryPoints);	
		smoothPath.setStroke(Color.RED);
		
		// Put the paths together and display them
		primaryStage.setScene(new Scene(new Group(origPath, smoothPath)));
		primaryStage.setWidth(WINDOW_WIDTH);
		primaryStage.setHeight(WINDOW_HEIGHT);
		primaryStage.resizableProperty().set(false);
		primaryStage.show();
	}
	
	// Given a list of points, make a path for drawing on the screen.
	private static Path makePath(List<Translation2d> pointsMeters) {
		// Scale the coordinates to use pixels instead of meters
		// We need to flip the Y coordinate
		List<Translation2d> pixelCoords = pointsMeters.stream()
				.map(t -> t.times(PIXELS_PER_METER))
				.map(t -> new Translation2d(t.getX(), WINDOW_HEIGHT - t.getY()))
				.collect(Collectors.toList());

		// A JavaFX Path consists of "MoveTo" and "LineTo" elements
		// We move to the first element, then draw lines to all the others
		List<PathElement> pathElems = Stream.concat(
				pixelCoords.stream().limit(1).map(t -> new MoveTo(t.getX(), t.getY())),
				pixelCoords.stream().map(t -> new LineTo(t.getX(), t.getY())))
				.collect(Collectors.toList());

		return new Path(pathElems);
	}

	// Given a row of a CSV file, make a Translation2d
	private static Translation2d recordToTranslation2d(CSVRecord record) {
		return new Translation2d(Double.parseDouble(record.get(0)), Double.parseDouble(record.get(1)));
	}

	public static void main(String[] args) {
		// This starts the JavaFX application, which in turn calls start()
		launch();
	}

}
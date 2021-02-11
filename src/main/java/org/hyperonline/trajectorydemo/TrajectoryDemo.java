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

	private Trajectory makeTrajectory(List<Pose2d> waypoints) {
		TrajectoryConfig config = new TrajectoryConfig(1, 1);
		
		return TrajectoryGenerator.generateTrajectory(waypoints, config);
	}

	@Override
	public void start(Stage primaryStage) throws Exception {
		// Read a list of points from a CSV file.
		Reader in = new FileReader("slalompath_nobom.csv");
		List<Pose2d> waypoints = CSVFormat.DEFAULT.parse(in).getRecords().stream()
				.map(TrajectoryDemo::recordToPose2d) // convert CSV records to Translation2d's
				.map(TrajectoryDemo::scalePoseToMeters) // scale so units are in meters
				.collect(Collectors.toList()); // collect results into a list

		if (waypoints.size() < 2) {
			throw new RuntimeException("Less than two waypoints provided");
		}
		
		Trajectory trajectory = makeTrajectory(waypoints);
		
		System.out.println(trajectory);
		
		// Get a list of points in the new trajecotry
		List<Translation2d> trajectoryPoints = trajectory.getStates().stream()
				.map(state -> state.poseMeters.getTranslation()).collect(Collectors.toList());

		// Plot both the original waypoints and the new trajectory as paths.
		// Path is part of JavaFX. It refers to a path drawn on the screen.
		Path origPath = makePathFromPose(waypoints);
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

	private static Path makePathFromPose(List<Pose2d> posesMeters) {
		return makePath(posesMeters.stream().map(Pose2d::getTranslation).collect(Collectors.toList()));
	}
	
	// Given a list of points, make a path for drawing on the screen.
	private static Path makePath(List<Translation2d> pointsMeters) {
		// Scale the coordinates to use pixels instead of meters
		// We need to flip the Y coordinate
		List<Translation2d> pixelCoords = pointsMeters.stream().map(t -> t.times(PIXELS_PER_METER))
				.map(t -> new Translation2d(t.getX(), WINDOW_HEIGHT - t.getY())).collect(Collectors.toList());

		// A JavaFX Path consists of "MoveTo" and "LineTo" elements
		// We move to the first element, then draw lines to all the others
		List<PathElement> pathElems = Stream
				.concat(pixelCoords.stream().limit(1).map(t -> new MoveTo(t.getX(), t.getY())),
						pixelCoords.stream().map(t -> new LineTo(t.getX(), t.getY())))
				.collect(Collectors.toList());

		return new Path(pathElems);
	}
	
	private static Pose2d scalePoseToMeters(Pose2d in) {
		return new Pose2d(in.getTranslation().times(Units.inchesToMeters(1)), in.getRotation());
	}

	// Given a row of a CSV file, make a Pose2d
	private static Pose2d recordToPose2d(CSVRecord record) {
		return new Pose2d(
				Double.parseDouble(record.get(0)), 
				Double.parseDouble(record.get(1)),
				Rotation2d.fromDegrees(Double.parseDouble(record.get(2))));
	}

	public static void main(String[] args) {
		// This starts the JavaFX application, which in turn calls start()
		launch();
	}

}

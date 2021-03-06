/**
 * 
 */
package replicatorg.app.ui.modeling;

import java.awt.Dimension;
import java.awt.Font;
import java.awt.GraphicsConfiguration;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.event.MouseWheelListener;
import java.util.logging.Level;

import javax.media.j3d.AmbientLight;
import javax.media.j3d.Appearance;
import javax.media.j3d.Background;
import javax.media.j3d.BoundingSphere;
import javax.media.j3d.BranchGroup;
import javax.media.j3d.Canvas3D;
import javax.media.j3d.ColoringAttributes;
import javax.media.j3d.DirectionalLight;
import javax.media.j3d.Font3D;
import javax.media.j3d.FontExtrusion;
import javax.media.j3d.GeometryArray;
import javax.media.j3d.Group;
import javax.media.j3d.LineArray;
import javax.media.j3d.LineAttributes;
import javax.media.j3d.Material;
import javax.media.j3d.Node;
import javax.media.j3d.OrientedShape3D;
import javax.media.j3d.PolygonAttributes;
import javax.media.j3d.QuadArray;
import javax.media.j3d.Shape3D;
import javax.media.j3d.Text3D;
import javax.media.j3d.Transform3D;
import javax.media.j3d.TransformGroup;
import javax.media.j3d.TransparencyAttributes;
import javax.media.j3d.View;
import javax.swing.JPanel;
import javax.vecmath.Color3f;
import javax.vecmath.Point3d;
import javax.vecmath.Vector3d;
import javax.vecmath.Vector3f;

import net.miginfocom.swing.MigLayout;
import replicatorg.app.Base;
import replicatorg.app.ui.MainWindow;
import replicatorg.model.BuildModel;

import com.sun.j3d.utils.universe.SimpleUniverse;

/**
 * @author phooky
 *
 */
public class PreviewPanel extends JPanel {

	BoundingSphere bounds =
		new BoundingSphere(new Point3d(0.0,0.0,0.0), 1000.0);

	EditingModel model = null;
	
	EditingModel getModel() { return model; }
	
	public void setModel(BuildModel buildModel) {
		if (model == null || buildModel != model.getBuildModel()) {
			if (buildModel != null) {
				model = new EditingModel(buildModel, mainWindow);
				setScene(model);
			} else {
				model = null;
			}
		}
	}

	private void setScene(EditingModel model) {
		Base.logger.info(model.model.getPath());
		if (objectBranch != null) {
			sceneGroup.removeChild(objectBranch);
		}
		objectBranch = model.getGroup();
		sceneGroup.addChild(objectBranch);
	}
	
	MainWindow mainWindow;

	enum DragMode {
		NONE,
		ROTATE_VIEW,
		TRANSLATE_VIEW,
		ROTATE_OBJECT,
		SCALE_OBJECT,
		TRANSLATE_OBJECT
	};

	ToolPanel toolPanel;
	
	Tool currentTool = null; // The tool currently in use.
	
	void setTool(Tool tool) {
		if (currentTool == tool) { return; }
		if (currentTool != null) {
			if (currentTool instanceof MouseListener) {
				canvas.removeMouseListener((MouseListener)currentTool);
			}
			if (currentTool instanceof MouseMotionListener) {
				canvas.removeMouseMotionListener((MouseMotionListener)currentTool);
			}
			if (currentTool instanceof MouseWheelListener) {
				canvas.removeMouseWheelListener((MouseWheelListener)currentTool);
			}
			if (currentTool instanceof KeyListener) {
				canvas.removeKeyListener((KeyListener)currentTool);
			}
		}
		currentTool = tool;
		if (currentTool != null) {
			if (currentTool instanceof MouseListener) {
				canvas.addMouseListener((MouseListener)currentTool);
			}
			if (currentTool instanceof MouseMotionListener) {
				canvas.addMouseMotionListener((MouseMotionListener)currentTool);
			}
			if (currentTool instanceof MouseWheelListener) {
				canvas.addMouseWheelListener((MouseWheelListener)currentTool);
			}
			if (currentTool instanceof KeyListener) {
				canvas.addKeyListener((KeyListener)currentTool);
			}
		}
	}
		
	Canvas3D canvas;
	
	void adjustViewAngle(double deltaYaw, double deltaPitch) {
		turntableAngle += deltaYaw;
		elevationAngle += deltaPitch;
		updateVP();
	}
	
	void adjustViewTranslation(double deltaX, double deltaY) {
		cameraTranslation.x += deltaX;
		cameraTranslation.y += deltaY;
		updateVP();
	}
	
	void adjustZoom(double deltaZoom) {
		cameraTranslation.z += deltaZoom;
		updateVP();
	}
	
	public PreviewPanel(final MainWindow mainWindow) {
		this.mainWindow = mainWindow;
		//setLayout(new MigLayout()); 
		setLayout(new MigLayout("fill,ins 0,gap 0"));
		// Create Canvas3D and SimpleUniverse; add canvas to drawing panel
		canvas = createUniverse();
		add(canvas, "growx,growy");
		toolPanel = new ToolPanel(this);
		if (Base.isMacOS()) {
			add(toolPanel,"dock east,width max(300,25%)");
		} else {
			add(toolPanel,"dock east,width max(200,20%)");
		}
		// Create the content branch and add it to the universe
		BranchGroup scene = createSTLScene();
		univ.addBranchGraph(scene);
		
		canvas.addKeyListener( new KeyListener() {
			public void keyPressed(KeyEvent e) {
				if (e.getKeyChar() == 'e') {
					showEdges = !showEdges;
					model.showEdges(showEdges);
				} else {
					return;
				}
				updateVP();
			}
			public void keyReleased(KeyEvent e) {
			}
			public void keyTyped(KeyEvent e) {
			}
		});
		
		addKeyListener(toolPanel);

	}		


	private SimpleUniverse univ = null;

	/**
	 * Indicates whether we're in edge (wireframe) mode.  False indicates a solid view. 
	 */
	private boolean showEdges = false;

	public Node makeAmbientLight() {
		AmbientLight ambient = new AmbientLight();
		ambient.setColor(new Color3f(0.3f,0.3f,0.9f));
		ambient.setInfluencingBounds(bounds);
		return ambient;
	}

	public Node makeDirectedLight1() {
		Color3f color = new Color3f(0.7f,0.7f,0.7f);
		Vector3f direction = new Vector3f(1f,0.7f,-0.2f);
		DirectionalLight light = new DirectionalLight(color,direction);
		light.setInfluencingBounds(bounds);
		return light;
	}

	public Node makeDirectedLight2() {
		Color3f color = new Color3f(0.5f,0.5f,0.5f);
		Vector3f direction = new Vector3f(-1f,-0.7f,0.2f);
		DirectionalLight light = new DirectionalLight(color,direction);
		light.setInfluencingBounds(bounds);
		return light;
	}

	final double wireBoxCoordinates[] = {
			0,  0,  0,    0,  0,  1,
			0,  1,  0,    0,  1,  1,
			1,  1,  0,    1,  1,  1,
			1,  0,  0,    1,  0,  1,

			0,  0,  0,    0,  1,  0,
			0,  0,  1,    0,  1,  1,
			1,  0,  1,    1,  1,  1,
			1,  0,  0,    1,  1,  0,

			0,  0,  0,    1,  0,  0,
			0,  0,  1,    1,  0,  1,
			0,  1,  1,    1,  1,  1,
			0,  1,  0,    1,  1,  0,
	};

	public Shape3D makeBoxFrame(Point3d ll, Vector3d dim) {
		Appearance edges = new Appearance();
		edges.setLineAttributes(new LineAttributes(0.9f,LineAttributes.PATTERN_SOLID,false));
		edges.setPolygonAttributes(new PolygonAttributes(PolygonAttributes.POLYGON_LINE,
				PolygonAttributes.CULL_NONE,0));
		double[] coords = new double[wireBoxCoordinates.length];
		for (int i = 0; i < wireBoxCoordinates.length;) {
			coords[i] = (wireBoxCoordinates[i] * dim.x) + ll.x; i++;
			coords[i] = (wireBoxCoordinates[i] * dim.y) + ll.y; i++;
			coords[i] = (wireBoxCoordinates[i] * dim.z) + ll.z; i++;
		}
		LineArray wires = new LineArray(wireBoxCoordinates.length/3,GeometryArray.COORDINATES);
		wires.setCoordinates(0, coords);

		return new Shape3D(wires,edges); 
	}

	Font3D labelFont = null;
	
	public Group makeLabel(String s, Vector3d where) {
		if (labelFont == null) {
			labelFont = new Font3D(Font.decode("Sans"), new FontExtrusion());
		}
		Text3D text = new Text3D(labelFont, s);
        TransformGroup tg = new TransformGroup();
        Transform3D transform = new Transform3D();
        transform.setTranslation(where);
        tg.setTransform(transform);
        OrientedShape3D os = new OrientedShape3D();
        os.setAlignmentAxis( 0.0f, 0.0f, 1.0f);
        os.setAlignmentMode(OrientedShape3D.ROTATE_ABOUT_POINT);
        os.setConstantScaleEnable(true);
        os.setScale(0.05);
        os.setGeometry(text);
        tg.addChild(os);
        return tg;
	}
	public Group makeAxes(Point3d origin) {
		Group g = new Group();
		g.addChild(makeLabel("X",new Vector3d(57,0,0)));
		g.addChild(makeLabel("Y",new Vector3d(0,57,0)));
		g.addChild(makeLabel("Z",new Vector3d(0d,0d,107)));
		return g;
	}
	
	private void loadPoint(Point3d point, double[] array, int idx) {
		array[idx] = point.x;
		array[idx+1] = point.y;
		array[idx+2] = point.z;
	}
		
	private Shape3D makePlatform(Point3d lower, Point3d upper) {
		Color3f color = new Color3f(1.0f,1.0f,1.0f); 
		Material m = new Material();
		m.setAmbientColor(color);
		m.setDiffuseColor(color);
		Appearance solid = new Appearance();
		solid.setTransparencyAttributes(new TransparencyAttributes(TransparencyAttributes.NICEST,0.6f));
		solid.setMaterial(m);
		PolygonAttributes pa = new PolygonAttributes();
		pa.setPolygonMode(PolygonAttributes.POLYGON_FILL);
		pa.setCullFace(PolygonAttributes.CULL_NONE);
		pa.setBackFaceNormalFlip(true);
	    solid.setPolygonAttributes(pa);

		double[] coords = new double[4*3];
		loadPoint(lower,coords,0);
		loadPoint(new Point3d(lower.x,upper.y,upper.z),coords,3);
		loadPoint(upper,coords,6);
		loadPoint(new Point3d(upper.x,lower.y,upper.z),coords,9);
			
		QuadArray plat = new QuadArray(4,GeometryArray.COORDINATES);
		plat.setCoordinates(0, coords);

		return new Shape3D(plat,solid); 
		
	}

	public Node makeBoundingBox() {

		Group boxGroup = new Group();
		Shape3D boxframe = makeBoxFrame(new Point3d(-50,-50,0), new Vector3d(100,100,100));	

		/*
		Appearance sides = new Appearance();
		sides.setTransparencyAttributes(new TransparencyAttributes(TransparencyAttributes.NICEST,0.9f));
		Color3f color = new Color3f(0.05f,0.05f,1.0f); 
		Material m = new Material(color,color,color,color,64.0f);
		sides.setMaterial(m);

		Box box = new Box(50,50,50,sides);
		Transform3D tf = new Transform3D();
		tf.setTranslation(new Vector3d(0,0,50));
		TransformGroup tg = new TransformGroup(tf);
		tg.addChild(box);
		tg.addChild(boxframe);
		*/
		boxGroup.addChild(boxframe);
		boxGroup.addChild(this.makePlatform(new Point3d(-50,-50,-0.001), new Point3d(50,50,-0.001)));
		return boxGroup;
	}

	public Node makeBackground() {
		Background bg = new Background(0.5f,0.5f,0.6f);
		bg.setApplicationBounds(bounds);
		return bg;
	}

	public Node makeBaseGrid() {
		Appearance edges = new Appearance();
		edges.setLineAttributes(new LineAttributes(1,LineAttributes.PATTERN_SOLID,true));
		edges.setColoringAttributes(new ColoringAttributes(0.6f,0.6f,0.8f,ColoringAttributes.FASTEST));
		final int LINES = 11;
		LineArray grid = new LineArray(4*(LINES-2),GeometryArray.COORDINATES);
		for (int i = 1; i < LINES-1; i++) {
			double offset = -50 + (100/(LINES-1))*i;
			int idx = (i-1)*4;
			// Along x axis
			grid.setCoordinate(idx++, new Point3d(offset,-50,0));
			grid.setCoordinate(idx++, new Point3d(offset,50,0));
			// Along y axis
			grid.setCoordinate(idx++, new Point3d(-50,offset,0));
			grid.setCoordinate(idx++, new Point3d(50,offset,0));
		}
		return new Shape3D(grid,edges); 
	}
	
	BranchGroup sceneGroup;
	BranchGroup objectBranch;
			
	/**
	 * Center the object and flatten the bottommost poly.  (A more thorough version would
	 * be able to correctly center a tripod or other spiky object.)
	 */
	public void align() {
		model.center();
	}
	
	
	public BranchGroup createSTLScene() {
		// Create the root of the branch graph
		BranchGroup objRoot = new BranchGroup();

		sceneGroup = new BranchGroup();
		sceneGroup.setCapability(BranchGroup.ALLOW_CHILDREN_EXTEND);
		sceneGroup.setCapability(BranchGroup.ALLOW_CHILDREN_WRITE);
		sceneGroup.addChild(makeAmbientLight());
		sceneGroup.addChild(makeDirectedLight1());
		sceneGroup.addChild(makeDirectedLight2());
		sceneGroup.addChild(makeBoundingBox());
		sceneGroup.addChild(makeBackground());
		sceneGroup.addChild(makeBaseGrid());

		objRoot.addChild(sceneGroup);

		// Create a new Behavior object that will perform the
		// desired operation on the specified transform and add
		// it into the scene graph.
		//	Transform3D yAxis = new Transform3D();
		//	Alpha rotationAlpha = new Alpha(-1, 4000);

		//	RotationInterpolator rotator =
		//	    new RotationInterpolator(rotationAlpha, objTrans, yAxis,
		//				     0.0f, (float) Math.PI*2.0f);
		//	BoundingSphere bounds =
		//	    new BoundingSphere(new Point3d(0.0,0.0,0.0), 100.0);
		//	rotator.setSchedulingBounds(bounds);
		objRoot.compile();

		return objRoot;
	}

	// These values were determined experimentally to look pretty dang good.
	final static Vector3d CAMERA_TRANSLATION_DEFAULT = new Vector3d(0,0,290);
	final static double ELEVATION_ANGLE_DEFAULT = 1.278;
	final static double TURNTABLE_ANGLE_DEFAULT = 0.214;
	
	final static double CAMERA_DISTANCE_DEFAULT = 300d; // 30cm
	
	Vector3d cameraTranslation = new Vector3d(CAMERA_TRANSLATION_DEFAULT);
	double elevationAngle = ELEVATION_ANGLE_DEFAULT;
	double turntableAngle = TURNTABLE_ANGLE_DEFAULT;

	Transform3D getViewTransform() {
		TransformGroup viewTG = univ.getViewingPlatform().getViewPlatformTransform();
		Transform3D t = new Transform3D();
		viewTG.getTransform(t);
		return t;
	}
	
//	double VIEW_SCALE = 100d;
	private void updateVP() {
		TransformGroup viewTG = univ.getViewingPlatform().getViewPlatformTransform();
		Transform3D t3d = new Transform3D();
		Transform3D trans = new Transform3D();
		Transform3D rotZ = new Transform3D();
		Transform3D rotX = new Transform3D();
		trans.setTranslation(cameraTranslation);
		Transform3D drop = new Transform3D();
		Transform3D raise = new Transform3D();
		drop.setTranslation(new Vector3d(0,0,50));
		raise.invert(drop);
		rotX.rotX(elevationAngle);
		rotZ.rotZ(turntableAngle);
		t3d.mul(drop);
		t3d.mul(rotZ);
		t3d.mul(rotX);
		t3d.mul(raise);
		t3d.mul(trans);
		viewTG.setTransform(t3d);

		if (Base.logger.isLoggable(Level.FINE)) {
			Base.logger.fine("Camera Translation: "+cameraTranslation.toString());
			Base.logger.fine("Elevation "+Double.toString(elevationAngle)+", turntable "+Double.toString(turntableAngle));
		}
	}

	private Canvas3D createUniverse() {
		// Get the preferred graphics configuration for the default screen
		GraphicsConfiguration config =
			SimpleUniverse.getPreferredConfiguration();

		// Create a Canvas3D using the preferred configuration
		Canvas3D c = new Canvas3D(config) {
			public Dimension getMinimumSize()
		    {
		        return new Dimension(0, 0);
		    }
		};

		// Create simple universe with view branch
		univ = new SimpleUniverse(c);
		univ.getViewer().getView().setSceneAntialiasingEnable(true);
		univ.getViewer().getView().setFrontClipDistance(10d);
		univ.getViewer().getView().setBackClipDistance(1000d);
		updateVP();

		// Ensure at least 5 msec per frame (i.e., < 200Hz)
		univ.getViewer().getView().setMinimumFrameCycleTime(5);

		return c;
	}

	void resetView() {
		elevationAngle = ELEVATION_ANGLE_DEFAULT;
		turntableAngle = TURNTABLE_ANGLE_DEFAULT;
		updateVP();
	}

	public void viewXY() {
		turntableAngle = 0d;
		elevationAngle = 0d;
		updateVP();	
	}
	
	public void viewYZ() {
		turntableAngle = 0d;
		elevationAngle = Math.PI/2;
		updateVP();	
	}
	public void viewXZ() {
		elevationAngle = Math.PI/2;
		turntableAngle = Math.PI/2;
		updateVP();	
	}
	
	public void usePerspective(boolean perspective) {
		univ.getViewer().getView().setProjectionPolicy(perspective?View.PERSPECTIVE_PROJECTION:View.PARALLEL_PROJECTION);
	}


}

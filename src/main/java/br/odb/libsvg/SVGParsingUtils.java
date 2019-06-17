/**
 * 
 */
package br.odb.libsvg;

import java.io.DataOutputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import br.odb.gameutils.Color;

/**
 * @author monty
 * 
 */
public final class SVGParsingUtils {

	public static final class Gradient {
		
		public Gradient() {
			
		}

		public Gradient(Gradient gradient) {
			
			this.id = gradient.id;
			this.link = gradient.link;
			this.x1 = gradient.x1;
			this.x2 = gradient.x2;
			this.y1 = gradient.y1;
			this.y2 = gradient.y2;
			 
			stops = new GradientStop[ gradient.stops.length ];
			
			for ( int c = 0; c < gradient.stops.length; ++c ) {
				this.stops[ c ] = new GradientStop( gradient.stops[ c ] );
			}
		}
		
		public String id;
		public String link;
		public GradientStop[] stops;
		public float x1;
		public float x2;
		public float y1;
		public float y2;

		public void copy(HashMap<String, Gradient> gradients) {
			
			if ( gradients.get( link ).link != null ) {
				gradients.get( link ).copy( gradients );
			}			
			
			stops = gradients.get( link ).stops;			
		}

	}

	public static final class GradientStop {
		
		public GradientStop() {			
		}
		
		public GradientStop(GradientStop gradientStop) {
			this.index = gradientStop.index;
			this.style = gradientStop.style;
		}
		
		public int index;
		public String style;
		public Color color;
	}

	public static void writeBinary(SVGGraphic processed, SVGGraphic original,
			String pathOutput) {
		FileOutputStream os = null;
		DataOutputStream dos = null;
		try {
			os = new FileOutputStream(pathOutput);
			dos = new DataOutputStream(os);
			ArrayList<ColoredPolygon> pols = new ArrayList<ColoredPolygon>(
					Arrays.asList(processed.shapes));
			ColoredPolygon pol;
			int size;

			size = original.shapes.length;
			dos.writeInt(size);

			for (int c = 0; c < size; ++c) {

				pol = original.shapes[c];
				pol.writeEdges(os);
			}

			size = pols.size();
			dos.writeInt(size);

			for (int c = 0; c < size; ++c) {

				pol = pols.get(c);
				pol.writePath(os);
			}
			System.out.println("wrote " + size + " polygons");

			os.close();
		} catch (Exception ex) {
			ex.printStackTrace();
		} finally {
			try {
				if (os != null)
					os.close();
			} catch (Exception ex) {
				ex.printStackTrace();
			}
		}
	}

	// -----------------------------------------------------------------------------
	/**
	 * 
	 * @param processed
	 * @param pathOutput
	 */
	public static void writeSVG(SVGGraphic processed, String pathOutput) {
		FileOutputStream os = null;
		try {
			ArrayList<ColoredPolygon> pols = new ArrayList<ColoredPolygon>();

			for (ColoredPolygon c : processed.shapes)
				pols.add(c);

			ColoredPolygon pol;

			os = new FileOutputStream(pathOutput);
			String head;
			// TODO: change those page size values;
			head = "<?xml version='1.0' encoding='UTF-8' standalone='no'?>\n<svg xmlns='http://www.w3.org/2000/svg'>\n<g>\n";
			os.write(head.getBytes());
			for (int c = 0; c < pols.size(); ++c) {
				pol = pols.get(c);
				pol.buildStyleProperty();
				head = pol.getSVGString();
				os.write(head.getBytes());
			}

			head = "</g>\n</svg>";
			os.write(head.getBytes());

			os.close();
		} catch (Exception ex) {
			ex.printStackTrace();
		} finally {
			try {
				if (os != null)
					os.close();
			} catch (Exception ex) {
				ex.printStackTrace();
			}
		}
	}

	// -----------------------------------------------------------------------------
	/**
	 * 
	 * @param proc4
	 * @return
	 */
	public static ArrayList<ColoredPolygon> triangulate(ColoredPolygon proc2) {
		ArrayList<ColoredPolygon> toReturn = new ArrayList<ColoredPolygon>();
		ColoredPolygon pol;
		for (int c = 0; c < proc2.npoints - 2; ++c) {
			pol = new ColoredPolygon();
			pol.color = proc2.color;
			pol.z = proc2.z;
			pol.originalStyle = proc2.originalStyle;
			toReturn.add(pol);
			pol.addPoint(proc2.xpoints[0], proc2.ypoints[0]);
			pol.addPoint(proc2.xpoints[c + 1], proc2.ypoints[c + 1]);
			pol.addPoint(proc2.xpoints[c + 2], proc2.ypoints[c + 2]);
		}

		return toReturn;
	}

	// -----------------------------------------------------------------------------
	/**
	 * 
	 * @param monotonic
	 * @return
	 */
	public static SVGGraphic splitIntoMonotones(SVGGraphic polytonics) {

		SVGGraphic toReturn = new SVGGraphic();
		ArrayList<ColoredPolygon> candidate = new ArrayList<ColoredPolygon>();
		ArrayList<ColoredPolygon> pols = new ArrayList<ColoredPolygon>();

		for (ColoredPolygon c : polytonics.shapes)
			pols.add(c);

		int[] xi;
		float[] xv;
		ColoredPolygon proc;
		ColoredPolygon proc1;
		ColoredPolygon proc2;
		ArrayList<ColoredPolygon> proc3;
		ColoredPolygon proc4;
		ArrayList<ColoredPolygon> proc5;
		ColoredPolygon proc6;
		int c;
		int f;
		int bigger = 0;

		for (int d = 0; d < pols.size(); ++d) {

			proc = pols.get(d);

			if (proc.npoints == 0) {
				System.out
						.println("*WARNING*: image contains degenerated polygon!");
				continue;
			}

			if (proc.npoints > bigger)
				bigger = proc.npoints;

			if (proc.npoints >= 35) {
				System.out
						.println("*WARNING*: as of this beta version, the number of verteces you're pushing is not supported");
			}
			xv = new float[proc.npoints];
			xi = new int[proc.npoints];

			for (int e = 0; e < proc.npoints; e++) {
				xv[e] = proc.xpoints[e];
				xi[e] = e;
			}

			SVGUtils.order(xv, xi);

			for (int h = 0; h < proc.npoints; h++) {
				c = xi[h];
				f = xi[(h + 1) % proc.npoints];

				proc1 = SVGUtils.CullXNoLessThan(proc.xpoints[c], proc);
				proc2 = SVGUtils.CullXNoMoreThan(proc.xpoints[f], proc1);
				proc3 = decomposeMonotone(proc2);

				for (int i = 0; i < proc3.size(); ++i) {
					proc4 = proc3.get(i);

					proc5 = triangulate(proc4);

					for (int j = 0; j < proc5.size(); ++j) {
						proc6 = proc5.get(j);
						candidate.add(proc6);
					}
				}
			}
		}

		System.out.println("bigger polygon has " + bigger);

		toReturn.fromArrayList(candidate);

		return toReturn;
	}

	// -----------------------------------------------------------------------------
	/**
	 * 
	 * @param proc2
	 * @return
	 */
	public static ArrayList<ColoredPolygon> decomposeMonotone(
			ColoredPolygon proc) {
		ColoredPolygon polygon = proc;
		ColoredPolygon p;
		float v1x;
		float v1y;
		float v2x;
		float v2y;
		float v3x;
		float v3y;
		float vx;
		float vy;
		int c = 0;
		ArrayList<ColoredPolygon> toReturn = new ArrayList<ColoredPolygon>();

		while (polygon.npoints > 4) {
			p = new ColoredPolygon();
			p.color = polygon.color;
			p.z = polygon.z;
			p.originalStyle = polygon.originalStyle;
			c = 0;
			v1x = polygon.xpoints[0];
			v1y = polygon.ypoints[0];

			vx = v1x;
			vy = v1y;

			while (c < polygon.npoints) {

				vx = polygon.xpoints[c];
				vy = polygon.ypoints[c];

				if (vx != v1x) {
					break;
				}

				v1x = vx;
				v1y = vy;

				c++;
			}

			p.addPoint(v1x, v1y);

			while (c < polygon.npoints) {

				vx = polygon.xpoints[c];
				vy = polygon.ypoints[c];

				if (vx != v1x) {
					break;
				}
				c++;
			}

			v2x = vx;
			v2y = vy;
			v3x = vx;
			v3y = vy;
			p.addPoint(v2x, v2y);

			while (c < polygon.npoints) {

				vx = polygon.xpoints[c];
				vy = polygon.ypoints[c];

				if (vx != v2x) {
					break;
				}

				v3x = vx;
				v3y = vy;

				c++;
			}

			p.addPoint(v3x, v3y);
			p.addPoint(vx, vy);

			toReturn.add(p);

			p = new ColoredPolygon();
			p.color = polygon.color;
			p.originalStyle = polygon.originalStyle;
			while (c < polygon.npoints) {
				vx = polygon.xpoints[c];
				vy = polygon.ypoints[c];
				p.addPoint(vx, vy);
				c++;
			}

			polygon = p;
		}

		toReturn.add(polygon);

		return toReturn;
	}

	// -----------------------------------------------------------------------------
	/**
	 * 
	 */
	public static SVGGraphic readSVG(InputStream is) {

		float x = 0;
		float y = 0;
		float width = 0;
		float height = 0;
		ArrayList<ColoredPolygon> instance = new ArrayList<ColoredPolygon>();
		SVGGraphic toReturn = new SVGGraphic();

		try {
			DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
			DocumentBuilder db = dbf.newDocumentBuilder();
			Document doc = db.parse(is);
			doc.getDocumentElement().normalize();
			Color color = null;
			ColoredPolygon p = null;
			NodeList nodeLst;
			String style;
			nodeLst = doc.getElementsByTagName("*");

			for (int s = 0; s < nodeLst.getLength(); s++) {

				Node fstNode = nodeLst.item(s);

				color = null;

				if (fstNode != null) {

					if (fstNode.getNodeType() == Node.ELEMENT_NODE
							&& (fstNode.getNodeName().equalsIgnoreCase("path")
									|| fstNode.getNodeName().equalsIgnoreCase(
											"rect") || fstNode.getNodeName()
									.equalsIgnoreCase("linearGradient")

							)) {

						Element fstElmnt = (Element) fstNode;
						style = fstElmnt.getAttribute("style");

						if (style != null && style.length() > 0)
							color = SVGUtils.parseColorFromStyle(fstElmnt
									.getAttribute("style"));

						if (color == null)
							color = new Color(0xFF000000);

						if (fstNode.getNodeName().equalsIgnoreCase("path")) {

							String d = null;

							NamedNodeMap properties = fstElmnt.getAttributes();

							for (int j = 0; j < properties.getLength(); j++) {
								Node property = properties.item(j);
								String name = property.getNodeName();

								if (name.equalsIgnoreCase("d")) {
									d = property.getNodeValue();
								}

								if (name.equalsIgnoreCase("style")) {
									style = property.getNodeValue();
								}
							}

							p = parsePath(d, style);

						} else if (fstNode.getNodeName().equalsIgnoreCase(
								"rect")) {
							p = new ColoredPolygon();

							if (fstElmnt.getAttribute("x").length() > 0)
								x = Float
										.parseFloat(fstElmnt.getAttribute("x"));

							if (fstElmnt.getAttribute("y").length() > 0)
								y = Float
										.parseFloat(fstElmnt.getAttribute("y"));

							if (fstElmnt.getAttribute("width").length() > 0)
								width = Float.parseFloat(fstElmnt
										.getAttribute("width"));

							if (fstElmnt.getAttribute("height").length() > 0)
								height = Float.parseFloat(fstElmnt
										.getAttribute("height"));

							p.addPoint(x, y);
							p.addPoint(x + width, y);
							p.addPoint(x + width, y + height);
							p.addPoint(x, y + height);
						} else if (fstNode.getNodeName().equalsIgnoreCase(
								"linearGradient")) {

							NodeList nl = fstNode.getChildNodes();
							Node stop;
							HashMap<Integer, GradientStop> stops = new HashMap<Integer, GradientStop>();
							NamedNodeMap data = fstNode.getAttributes();

							Gradient g = new Gradient();
							g.id = data.getNamedItem("id").getNodeValue();

							if (data.getNamedItem("xlink:href") != null) {
								
								// 0:#
								g.link = data.getNamedItem("xlink:href")
										.getNodeValue().substring(1);
							}

							if (data.getNamedItem("x1") != null) {
								
								g.x1 = Float.parseFloat(data.getNamedItem("x1")
										.getNodeValue());
							}

							if (data.getNamedItem("y1") != null) {
								
								g.y1 = Float.parseFloat(data.getNamedItem("y1")
										.getNodeValue());
							}

							if (data.getNamedItem("x2") != null) {
								
								g.x2 = Float.parseFloat(data.getNamedItem("x2")
										.getNodeValue());
							}

							if (data.getNamedItem("y2") != null) {
								
								g.y2 = Float.parseFloat(data.getNamedItem("y2")
										.getNodeValue());
							}

							for (int c = 0; c < nl.getLength(); ++c) {

								stop = nl.item(c);
								if (stop.getNodeName().equalsIgnoreCase("stop")) {

									NamedNodeMap attr = stop.getAttributes();

									GradientStop gs = new GradientStop();
									attr.getNamedItem("id").getNodeValue();
									gs.index = Integer.parseInt(attr
											.getNamedItem("offset")
											.getNodeValue());
									gs.style = attr.getNamedItem("style")
											.getNodeValue();
									stops.put(Integer.valueOf(gs.index), gs);
								}
							}

							if (stops.size() > 0) {

								g.stops = new GradientStop[stops.size()];

								for (int c = 0; c < stops.size(); ++c) {

									g.stops[c] = stops.get(Integer.valueOf(c));
								}

								stops.clear();
							}

							toReturn.gradients.put(g.id, g);
						}

						if (p != null) {

							if (fstElmnt != null) {

								if (fstElmnt.hasAttribute("z"))
									p.z = (int) Float.parseFloat(fstElmnt
											.getAttribute("z"));

								if (fstElmnt.hasAttribute("id"))
									p.id = fstElmnt.getAttribute("id");
							}

							p.color = color;
							p.originalStyle = fstElmnt.getAttribute("style");
							instance.add(p);
						}
					}
				}
			}
		} catch (Exception ex) {
			ex.printStackTrace();
		}

		toReturn.shapes = new ColoredPolygon[instance.size()];
		instance.toArray(toReturn.shapes);
		
		consolidateGradients( toReturn );

		return toReturn; 
	}
	
	private static void consolidateGradients( SVGGraphic graphic ) {
		for ( Gradient g : graphic.gradients.values() ) {
			if ( g.link != null ) {
				g.copy( graphic.gradients );
			}
		}
	}

	private static ColoredPolygon parsePath(String nodeValue, String style) {

		ColoredPolygon pol = SVGUtils.parseD(nodeValue, 800, 480);
		pol.color = SVGUtils.parseColorFromStyle(style);
		pol.originalStyle = style;

		return pol;
	}
}

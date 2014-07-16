package br.odb.libsvg;

import java.io.DataOutputStream;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.util.ArrayList;

import br.odb.utils.Color;
import br.odb.utils.math.Vec2;

/**
 * 
 * @author Daniel "Monty" Monteiro
 */
public class ColoredPolygon {
	
	public Color color = new Color();
	final private ArrayList<Vec2> points = new ArrayList<Vec2>();
	final public ArrayList<Vec2> controlPoints = new ArrayList<Vec2>();
	public String originalStyle;
	public float[] xpoints;
	public float[] ypoints;
	public int z = 0;
	public int npoints;
	public String id;
	public boolean visible = true;


	public String getSVGString() {
		int n;
		String toReturn = "";

		if (this.npoints <= 0)
			return toReturn;

		toReturn = "<path ";
		toReturn += "z = '" + z + "' ";
		toReturn += " style='";
		toReturn += originalStyle;

		if (id != null) {

			toReturn += "' Id='";
			toReturn += id;
		}

		toReturn += "' d='";
		toReturn += " M ";
		toReturn += xpoints[0];
		toReturn += ",";
		toReturn += ypoints[0];

		for (int c = 0; c < npoints; ++c) {

			n = c % (this.npoints);
			toReturn += " L ";
			toReturn += (this.xpoints[n]);
			toReturn += ",";
			toReturn += (this.ypoints[n]);
		}

		toReturn += " z' />\n";
		return toReturn;
	}

	public void writePath(OutputStream os) {

		byte[] bytes = new byte[11];
		bytes[0] = encodeUnsingedValueIntoByte(color.getA());
		bytes[1] = encodeUnsingedValueIntoByte(color.getR());
		bytes[2] = encodeUnsingedValueIntoByte(color.getG());
		bytes[3] = encodeUnsingedValueIntoByte(color.getB());

		bytes[4] = normalize(xpoints[0], 0, 800);
		bytes[5] = normalize(ypoints[0], 0, 480);

		bytes[6] = normalize(xpoints[1], 0, 800);
		bytes[7] = normalize(ypoints[1], 0, 480);

		bytes[8] = normalize(xpoints[2], 0, 800);
		bytes[9] = normalize(ypoints[2], 0, 480);

		bytes[10] = encodeUnsingedValueIntoByte(z);

		try {
			os.write(bytes);
		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}

	public void addPoint(float x, float y) {
		Vec2 p = new Vec2();
		p.x = x;
		p.y = y;
		points.add(p);
		p = new Vec2();
		controlPoints.add(p);
		p.invalidate();
		updateState();
	}

	private void updateState() {
		npoints = points.size();

		xpoints = new float[npoints];
		ypoints = new float[npoints];
		Vec2 v;

		for (int c = 0; c < npoints; ++c) {
			v = points.get(c);
			xpoints[c] = v.x;
			ypoints[c] = v.y;
		}
	}

	public void buildStyleProperty() {

		if (color == null)
			return;

		originalStyle = "fill:" + color.getHTMLColor() + ";";

		if (color.getA() != 255) {
			originalStyle += "opacity: " + (color.getA() / 255.0f) + ";";
		}
	}

	public void writeEdges(FileOutputStream os) {

		byte[] bytes = new byte[9];
		DataOutputStream dos = new DataOutputStream(os);

		try {

			dos.writeInt(npoints);

			for (int c = 0; c < npoints; ++c) {

				bytes[0] = encodeUnsingedValueIntoByte(color.getA());
				bytes[1] = encodeUnsingedValueIntoByte(color.getR());
				bytes[2] = encodeUnsingedValueIntoByte(color.getG());
				bytes[3] = encodeUnsingedValueIntoByte(color.getB());

				bytes[4] = normalize(xpoints[c % npoints], 0, 800);
				bytes[5] = normalize(ypoints[c % npoints], 0, 480);

				bytes[6] = normalize(xpoints[(c + 1) % npoints], 0, 800);
				bytes[7] = normalize(ypoints[(c + 1) % npoints], 0, 480);

				bytes[8] = encodeUnsingedValueIntoByte(z);

				os.write(bytes);
			}

		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}

	private byte encodeUnsingedValueIntoByte(int uv) {

		return (byte) (uv - Byte.MIN_VALUE);
	}

	// O ideal seria pegar o maior e o menor dos valores e usar como minimum e
	// maximum
	private byte normalize(float f, float minimum, float maximum) {

		int range = Byte.MAX_VALUE - Byte.MIN_VALUE;

		// f ������������������ primeiro normalizado, para depois ser
		// transformado para uma
		// nova base de espa������������������o vetorial
		return encodeUnsingedValueIntoByte((int) ((f * range) / maximum));
	}

	public Vec2 getCenter() {

		Vec2 v;
		Vec2 center = new Vec2();

		for (int c = 0; c < npoints; ++c) {
			v = points.get(c);
			center.x += v.x;
			center.y += v.y;
		}
		center.x /= npoints;
		center.y /= npoints;

		return center;
	}

	public ColoredPolygon scale(float width, float height) {
		
		ColoredPolygon toReturn = new ColoredPolygon();
		
		toReturn.color.set( color );
		toReturn.originalStyle = originalStyle;
		toReturn.z = z;
		toReturn.id = id;
		toReturn.visible = visible;

		for ( Vec2 p : points ) {
			toReturn.points.add( new Vec2( p ).scaled(  width, height) );
		}
		
		for ( Vec2 c : controlPoints ) {
			toReturn.controlPoints.add( new Vec2( c ).scaled( width, height ) );
		}
		
		toReturn.updateState();
		
		return toReturn;
	}
}

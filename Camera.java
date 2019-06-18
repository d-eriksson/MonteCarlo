import javax.vecmath.Vector3d;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;
import java.io.*;
import java.util.*;
public class Camera  {
    int Width;
    Vector3d eye;
    int subpixels;
    double fov;
    ColorDbl[][] pixelList;
    Camera(int w, int s, Vector3d e, double f){
        Width = w;
        subpixels = s;
        eye = e;
        fov = f;
        pixelList = new ColorDbl[w][w];
    }
    //Start rendering scene S
    void render(Scene S){
        long endTime;
        double PixelSize = 2.0/Width;
        double subPixelSize = PixelSize/subpixels;
        Vector3d endPoint;
        Ray r;
        ColorDbl temp;
        for(int j = 0; j < Width; ++j){
            for(int i = 0; i < Width; ++i){
                temp = new ColorDbl();
                for(int k = 0; k<subpixels; ++k){
                    for(int l = 0; l<subpixels; ++l){
                        endPoint = new Vector3d(eye.x+fov, i*PixelSize + 0.5*subPixelSize+k*subPixelSize - 1 + eye.y, -j*PixelSize - 0.5*subPixelSize-l*subPixelSize + 1 + eye.z);
                        r = new Ray(eye, endPoint, true);
                        temp.sumColor(r.CastRay(S,0,0));
                    }
                }
                temp.divide(subpixels*subpixels);
                temp.clamp();
                pixelList[i][j] = temp;
            }
            Utilities.updateProgress( (double) j/Width);
        }
    }
    //Write data to a PNG
    void write(String filename){
        File file = null;
        BufferedImage img = new BufferedImage(Width,Width, BufferedImage.TYPE_INT_RGB);
        for(int y = 0; y < Width; y++){
            for(int x = 0; x < Width; x++){
                int rgb = pixelList[x][y].RGBForImage();
                img.setRGB(x,y,rgb);
            }
        }
        try{
            file = new File(filename + ".png");
            ImageIO.write(img,"png",file);
        }
        catch(IOException e){
            System.out.println("Error: " +e );
        }
    }

    public static void main(String[] args) throws IOException{
        long startTime = System.nanoTime();

        //Create Scene and Camera
        Settings setting = new Settings();
        setting.setMaxDepth(1);
        setting.setChildren(16);
        setting.setDepthDecay(0.3);
        setting.setShadowRays(8);
        setting.setMaxReflectionBounces(8);
        Scene s = new Scene(setting);
        Camera c = new Camera(800, 2, new Vector3d(2.0,2.5,0.0),2.0);

        //Add objects to scene
        Sphere ball1 = new Sphere(new Vector3d(10.0, 3.0, 0.0), 1.0, new Reflective(new ColorDbl(1.0, 1.0, 1.0)));
        Sphere ball2 = new Sphere(new Vector3d(10.0, 0.0, 0.0), 1.0, new Reflective(new ColorDbl(1.0, 1.0, 1.0)));
        s.addObject(ball1);
        s.addObject(ball2);
        Tetrahedron T1 = new Tetrahedron(new Vector3d(9.0, -4.0, 3.0), 2.0, new Material(new ColorDbl(1.0, 0.0, 0.0)));
        //Box T2 = new Box(new Vector3d(9.0, 2.0, -4.0), 10.0, 7.0, 4.0, new Material(new ColorDbl(0.4, 1.0, 0.2)));
        Tetrahedron T3 = new Tetrahedron(new Vector3d(6.0, 2.0, -5.0), 2.0, new Material(new ColorDbl(0.4, 0.7, 1.0)));
        s.addObject(T1);
        //s.addObject(T2);
        s.addObject(T3);

        //Start rendering
        c.render(s);
        if(args.length > 0){
          c.write(args[0]);
        }
        else{
          c.write("image");
        }

        //Program ends here, set progress to 100%
        long endTime = System.nanoTime();
        Utilities.updateProgress( 1.0 );
        System.out.println("\nExecution time: " + (endTime-startTime)/1000000000.0+"s");
    }
}

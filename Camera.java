import javax.vecmath.Vector3d;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;
import java.io.*;
import java.util.*;
import java.util.concurrent.CountDownLatch;
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
    void draw(){
        BufferedImage img = new BufferedImage(Width,Width, BufferedImage.TYPE_INT_RGB);
        for(int y = 0; y < Width; y++){
            for(int x = 0; x < Width; x++){
                int rgb = pixelList[x][y].RGBForImage();
                img.setRGB(x,y,rgb);
            }
        }
    }
    public static void main(String[] args) throws IOException{
        long startTime = System.nanoTime();

        //Create Camera
        Camera c = new Camera(1080, Integer.parseInt(args[5]), new Vector3d(-2.0,0.0,0.0),1.25);
        Settings setting = new Settings();
        setting.setChildren(Integer.parseInt(args[0]));
        setting.setDepthDecay(Double.parseDouble(args[1]));
        setting.setShadowRays(Integer.parseInt(args[2]));
        setting.setMaxReflectionBounces(Integer.parseInt(args[3]));
        setting.setMaxDepth(Integer.parseInt(args[4]));

        //Create Scenes for each thread
        int threads = Runtime.getRuntime().availableProcessors();
        System.out.println("Found " + threads + " CPU cores.");
        CountDownLatch latch = new CountDownLatch(threads);
        int range = c.Width/threads;
        int remainder = 0;
        Scene[] Scenes = new Scene[threads];
        for(int i=0; i<threads; ++i){
            Scenes[i] = new Scene(new Settings(setting), c);
            Scenes[i].addObject(new Sphere(new Vector3d(9.0, -1.1, 0.2), 1.0, new Reflective(new ColorDbl(0.9, 0.9, 0.9))));
            Scenes[i].addObject(new Sphere(new Vector3d(9.0, 1.1, 0.2), 1.0, new Reflective(new ColorDbl(0.9, 0.9, 0.9))));
            Scenes[i].addObject(new Box(new Vector3d(9.0, 0.0, -2.9999), 4.0, 4.0, 4.0, new Material(new ColorDbl(0.95, 0.95, 0.95))));
            Scenes[i].addObject(new Sphere(new Vector3d(5.5, -4.1, 2.2), 0.5, new Reflective(new ColorDbl(0.9, 0.9, 0.9))));
            Scenes[i].addObject(new Sphere(new Vector3d(7.7, -4.1, -3.2), 0.5, new Material(new ColorDbl(0.9, 0.9, 0.9))));
            Scenes[i].addObject(new Sphere(new Vector3d(9.2, 3.6, -1.6), 0.5, new Reflective(new ColorDbl(0.9, 0.9, 0.9))));
            Scenes[i].addObject(new Sphere(new Vector3d(5.1, 4.4, 3.5), 0.5, new Reflective(new ColorDbl(0.9, 0.9, 0.9))));
            Scenes[i].addObject(new Sphere(new Vector3d(6.5, 3.1, -3), 0.5, new Material(new ColorDbl(0.9, 0.9, 0.9))));
            Scenes[i].addObject(new Sphere(new Vector3d(7.2, -4.7, -4.0), 0.5, new Reflective(new ColorDbl(0.9, 0.9, 0.9))));
            if(i==(threads-1)){remainder = c.Width%threads;}
            Thread T = new Thread(new Multithread(Scenes[i], range*i, range*(i+1)+remainder, i, latch));
            T.start();
        }

        //Wait for threads
        try{
            latch.await();
        }
        catch(InterruptedException e){}

        //Write file
        c.write("C" + args[0] + "-DD"  + args[1] + "-SR"  + args[2] + "-RB"  + args[3] + "-MD"  + args[4] + "-AA"  + args[5]);
        c.draw();

        long endTime = System.nanoTime();
        System.out.println("\nExecution time: " + (endTime-startTime)/1000000000.0+"s");
    }
}

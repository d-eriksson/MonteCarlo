import javax.vecmath.Vector3d;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;
import java.io.*;
import java.util.*;
import java.util.concurrent.CountDownLatch;
import javax.swing.JPanel;
import javax.swing.JFrame;
import javax.swing.Timer;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Dimension;
import java.awt.event.*;

public class Camera extends JPanel implements ActionListener {
    int Width;
    Vector3d eye;
    int subpixels;
    double fov;
    ColorDbl[][] pixelList;
    int progress;
    BufferedImage bimg;
    Timer timer = new Timer(100, this);

    Camera(int w, int s, Vector3d e, double f){
        Width = w;
        subpixels = s;
        eye = e;
        fov = f;
        progress = 0;
        pixelList = new ColorDbl[w][w];
        bimg = new BufferedImage(w, w, BufferedImage.TYPE_INT_RGB);
    }

    //JPanel code
    @Override
    public void paintComponent(Graphics g){
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g.create();
        g2d.drawImage(bimg, 0, 0, this);
        g2d.dispose();
    }
    //Update JPanel
    public void actionPerformed(ActionEvent ev){
        if(ev.getSource()==timer){
            repaint();// this will call at every 1 second
            updateProgress();
        }
    }

    //Update progress
    public void updateProgress(){
        final int w = 50; // progress bar width in chars
        double progressPercentage = ((double)progress)/Width;
        System.out.print("\r[");
        int i = 0;
        for (; i <= (int)(progressPercentage*w); i++) {
          System.out.print(".");
        }
        for (; i < w; i++) {
          System.out.print(" ");
        }
        System.out.print("]" + (int)(progressPercentage*100) + "%");
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

        //Create Camera
        Camera c = new Camera(800, Integer.parseInt(args[5]), new Vector3d(-2.0,0.0,0.0),1.25);
        Settings setting = new Settings();
        setting.setChildren(Integer.parseInt(args[0]));
        setting.setDepthDecay(Double.parseDouble(args[1]));
        setting.setShadowRays(Integer.parseInt(args[2]));
        setting.setMaxReflectionBounces(Integer.parseInt(args[3]));
        setting.setMaxDepth(Integer.parseInt(args[4]));

        //Create JFrame
        JFrame frame = new JFrame("Rendering preview");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setPreferredSize(new Dimension(c.Width,c.Width));
        frame.add(c);
        frame.pack();
        frame.setVisible(true);

        //Create independent identical scenes for each thread
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

        //Start JFrame refreshing
        c.timer.start();
        //Wait for threads
        try{
            latch.await();
        }
        catch(InterruptedException e){}
        //Stop JFrame refreshing
        c.timer.stop();

        //Write file
        c.write("C" + args[0] + "-DD"  + args[1] + "-SR"  + args[2] + "-RB"  + args[3] + "-MD"  + args[4] + "-AA"  + args[5]);

        long endTime = System.nanoTime();
        System.out.println("\nExecution time: " + (endTime-startTime)/1000000000.0+"s");
    }
}

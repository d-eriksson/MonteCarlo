import java.util.concurrent.CountDownLatch;
class Multithread implements Runnable {
  Scene scene;
  int start;
  int end;
  int thread_index;
  private CountDownLatch latch;
  Multithread(Scene s, int st, int e, int i, CountDownLatch l){
      scene = s;
      start = st;
      end = e;
      thread_index = i;
      latch = l;
  }
  public void run(){
      try{
          // Do something
          scene.render(start, end);
          latch.countDown();
      }
      catch (Exception e){System.out.println ("Exception is caught");}
  }
}

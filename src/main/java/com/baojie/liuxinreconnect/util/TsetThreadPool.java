package com.baojie.liuxinreconnect.util;

import java.util.concurrent.atomic.AtomicInteger;

public class TsetThreadPool {
	 private final AtomicInteger ctl = new AtomicInteger(ctlOf(RUNNING, 0));
	    private static final int COUNT_BITS = Integer.SIZE - 3;
	    private static final int CAPACITY   = (1 << COUNT_BITS) - 1;

	    // runState is stored in the high-order bits
	    private static final int RUNNING    = -1 << COUNT_BITS;
	    private static final int SHUTDOWN   =  0 << COUNT_BITS;
	    private static final int STOP       =  1 << COUNT_BITS;
	    private static final int TIDYING    =  2 << COUNT_BITS;
	    private static final int TERMINATED =  3 << COUNT_BITS;

	    // Packing and unpacking ctl
	    private static int runStateOf(int c)     { return c & ~CAPACITY; }
	    private static int workerCountOf(int c)  { return c & CAPACITY; }
	    private static int ctlOf(int rs, int wc) { return rs | wc; }


	    
	    
	    public void print(){
	    	System.out.println("ctl :"+ctl.get());
	    	System.out.println("COUNT_BITS :"+COUNT_BITS);
	    	System.out.println("CAPACITY :"+CAPACITY);
	    	System.out.println("RUNNING :"+RUNNING);
	    	System.out.println("SHUTDOWN :"+SHUTDOWN);
	    	System.out.println("STOP :"+STOP);
	    	System.out.println("TIDYING :"+TIDYING);
	    	System.out.println("TERMINATED :"+TERMINATED);
	    	System.out.println("runStateOf :"+runStateOf(100));
	    	System.out.println("workerCountOf :"+workerCountOf(200));
	    	System.out.println("ctlOf :"+ctlOf(100,200));
	    	
	    	
	    	
	    }
	    
	    
	    
	    public static void main(String args[]){
	    	TsetThreadPool threadPool=new TsetThreadPool();
	    	
	    	threadPool.print();
	    	
	    	int i=2;
	    	
	    	if(i==0){
	    		System.out.println(i);
	    	}else if (i==1) {
	    		System.out.println(i);
			}else if (i==2) {
				if(i>3){
					System.out.println(i);
				}else if (i>1) {
					System.out.println(i);
				}
				
			}else if (i==3) {
				System.out.println(i);
			}else if (i==4) {
				System.out.println(i);
			}else {
				System.out.println(i);
			}
	    	
	    	
	    	
	    	
	    	
	    	
	    }
	    
}

package examples;

import jpvm.Environment;
import jpvm.JPVMException;
import jpvm.buffer.Buffer;
import jpvm.messages.Message;
import jpvm.tasks.TaskId;

import java.util.Date;

/* sor.c
 *
 * Perform SOR on a poisson system using a red-black ordering of the
 * unknowns to allow parallel execution.
 *
 * Adam J Ferrari
 * Tue Apr  2 19:32:36 EST 1996
 * Ported to JPVM: Tue Mar 31 16:42:39 EST 1998
 */

class sor {
    static final double DEFAULT_MAX_ERROR = 0.01;
    static final int DEFAULT_NUM_PROCS = 1;
    static final int NOBODY = -1;
    static final double PI = 3.141592654;

    /* Convergence value for the error      */
    static double max_err = DEFAULT_MAX_ERROR;
    static int max_iterations = -1;

    static boolean verbose = false;

    /* Number of processes                  */
    static int nprocs = DEFAULT_NUM_PROCS;

    /* 2-D Mesh dimension of processes      */
    static int procdim;

    /* The task identifiers of all workers  */
    static TaskId tids[];
    static TaskId childTids[];

    /* Location in mesh of process          */
    static int my_id = -1, my_x = -1, my_y = -1;

    /* Numbers of my process neighbors      */
/* on each side.                        */
    static int east_id, west_id;
    static int north_id, south_id;


    /* True if the lower left hand corner   */
/* is red, false if it is black         */
    static boolean red_corner;

    /* Unknowns form an NxN grid            */
    static int N;

    /* There are n unknowns in all (N*N)    */
    static int n;

    /* Each process holds an NpxNp sub-grid */
    static int Np;

    /* Each process holds np unknowns in all */
    static int np;

    /* The local sub-grid of unknowns       */
    static double X[][];

    /* The local sub-grid of the RHS        */
    static double B[][];

    static double tmpBorder[];

    /* Number of iterations so far          */
    static int iteration = 0;

    /* Timing information                   */
    static double start_time, end_time;

    static final int PARAM_TAG = 10000;
    static final int DONE_TAG = 20000;
    static final int DOTPROD_TAG = 30000;
    static final int RED_NORTH_TAG = 40000;
    static final int RED_SOUTH_TAG = 50000;
    static final int RED_EAST_TAG = 60000;
    static final int RED_WEST_TAG = 70000;
    static final int BLACK_NORTH_TAG = 80000;
    static final int BLACK_SOUTH_TAG = 90000;
    static final int BLACK_EAST_TAG = 10000;
    static final int BLACK_WEST_TAG = 11000;

    static Environment jpvm = null;

    public static void main(String args[]) {
        try {
            jpvm = new Environment();
            doSor(args);
        } catch (JPVMException ex) {
            error("jpvm Exception - " + ex.toString());
        } catch (Exception e) {
            error("Exception - " + e.toString());
        }
    }

    public static void error(String message, boolean die) {
        System.err.println("sor: " + message);
        if (die) {
            if (jpvm != null) {
                try {
                    jpvm.pvm_exit();
                } catch (JPVMException ex) {
                }
                System.exit(1);
            }
        }
    }

    public static void warning(String message) {
        if (verbose) {
            if (my_id >= 0)
                System.out.println("sor (" + tids[my_id] + "): " + message);
            else
                System.out.println("sor: " + message);
        }
    }

    public static void error(String message) {
        error(message, true);
    }

    public static void usage() {
        error("usage -  java sor <grid dim> [-p <num tasks>] [-i <max iters>]");
    }

    public static double msecond() {
        Date d = new Date();
        double msec = (double) d.getTime();
        return msec;
    }

    public static void doSor(String args[])
            throws JPVMException {
        int i;
        TaskId my_tid;

        my_tid = jpvm.pvm_mytid();

        if (jpvm.pvm_parent() != jpvm.PvmNoParent) {
            slave_process(my_tid);
            return;
        }

        if (args.length < 1) {
            usage();
        }

        try {
            N = Integer.parseInt(args[0]);
            for (i = 1; i < args.length; i++) {
                if (args[i].equals("-p"))
                    nprocs = Integer.parseInt(args[++i]);
                else if (args[i].equals("-i"))
                    max_iterations = Integer.parseInt(args[++i]);
                else usage();
            }
        } catch (NumberFormatException e) {
            usage();
        }

	/* Verify that arguments make sense */
        procdim = (int) Math.sqrt((double) nprocs);
        if (nprocs != (procdim * procdim)) {
            error("num procs must be an even square!", true);
        }
        n = N * N;
        np = n / nprocs;
        if (np * nprocs != n) {
            error("num procs must divide problem size!", true);
        }
        Np = N / procdim;
        if (Np * procdim != N) {
            error("proc dim must divide problem size!", true);
        }
        if (Np * Np != np) {
            error("sub-blocks must be square!");
        }

        System.out.println("" + N + "x" + N + " red/black SOR poisson, max error=" +
                max_err + ", nprocs=" + nprocs);

	/* Start up the worker processes */
        master_process(my_tid);
    }

    public static void master_process(TaskId my_tid)
            throws JPVMException {
        int spawned;
        int i;

	/* Initialize my location information */
        tids = new TaskId[nprocs];
        childTids = new TaskId[nprocs - 1];
        tids[0] = my_tid;
        my_id = my_x = my_y = 0;
        init_NEWS();

	/* Spawn workers */
        if (nprocs > 1) {
            jpvm.pvm_spawn("sor", nprocs - 1, tids);
            tids[nprocs - 1] = tids[0];
            for (i = 0; i < (nprocs - 1); i++)
                childTids[i] = tids[i];
            tids[0] = my_tid;

		/* Distribute parameters to workers */
            Buffer buf = new Buffer();
            buf.pack(N);
            buf.pack(n);
            buf.pack(Np);
            buf.pack(np);
            buf.pack(nprocs);
            buf.pack(procdim);
            buf.pack(max_iterations);
            buf.pack(max_err);
            buf.pack(tids, nprocs, 1);
            jpvm.pvm_mcast(buf, childTids, nprocs - 1, PARAM_TAG);
        }

	/* Run the red/black SOR algorithm... */
        start_time = msecond();
        SOR_redblack();
        end_time = msecond();

//	for(i=1;i<nprocs;i++) {
//		warning("Blocking for "+DONE_TAG);
//		Message m = jpvm.pvm_recv(tids[i],DONE_TAG);
//	}
        System.out.println("Total run time = " + (end_time - start_time) +
                " msecs.");
        jpvm.pvm_exit();
    }

    public static void slave_process(TaskId my_tid)
            throws JPVMException {
    /* Recieve my initialization parameters */
        warning("Blocking for " + PARAM_TAG);
        Message m = jpvm.pvm_recv(PARAM_TAG);
        N = m.buffer.upkint();
        n = m.buffer.upkint();
        Np = m.buffer.upkint();
        np = m.buffer.upkint();
        nprocs = m.buffer.upkint();
        procdim = m.buffer.upkint();
        max_iterations = m.buffer.upkint();
        max_err = m.buffer.upkdouble();
        tids = new TaskId[nprocs];
        m.buffer.unpack(tids, nprocs, 1);

	/* Initialize my mesh location */
        for (my_id = 0; my_id < nprocs && (!(tids[my_id].equals(my_tid))); my_id++) ;
        my_x = my_id % procdim;
        my_y = my_id / procdim;
        init_NEWS();
	
	/* Run the red/black SOR algorithm... */
        start_time = msecond();
        SOR_redblack();
        end_time = msecond();

	/* Send timing stats to master */
        Buffer buf = new Buffer();
        buf.pack(iteration);
        jpvm.pvm_send(buf, jpvm.pvm_parent(), DONE_TAG);
        jpvm.pvm_exit();
    }

    static public void init_NEWS()
            throws JPVMException {
        int even_proc, even_Np;
	/* Determine my neighbors */
        if (my_y == (procdim - 1)) north_id = NOBODY;
        else north_id = my_id + procdim;
        if (my_y == 0) south_id = NOBODY;
        else south_id = my_id - procdim;
        if (my_x == (procdim - 1)) east_id = NOBODY;
        else east_id = my_id + 1;
        if (my_x == 0) west_id = NOBODY;
        else west_id = my_id - 1;

	/* Determine if my lower left hand corner is red or black */
        red_corner = false;

	/* If there are an even number of columns in each sub grid, */
	/* then every process has a red lower left corner.          */
        if ((Np & 0x1) == 0) {
            red_corner = true;
        } else {
		/* There are an odd number of columns in each sub grid */
            if ((procdim & 0x1) != 0) {
			/* If there are an odd number of processes in each */
			/* dimension of the grid, only even processes have */
			/* red corners                                     */
                if ((my_id & 0x1) == 0) red_corner = true;
            } else {
			/* If there are an even number of processes in each   */
			/* dimension of the process mesh, then even processes */
			/* in even rows, and odd processes in odd rows have   */
			/* red corners                                        */
                if (((my_x & 0x1) == 0) && ((my_y & 0x1) == 0)) red_corner = true;
                if (((my_x & 0x1) != 0) && ((my_y & 0x1) != 0)) red_corner = true;
            }
        }
    }

    static void SOR_redblack()
            throws JPVMException {
        int i, j;

	/* Initialize unknowns and the right hand side */
        init_unknowns();

        while (true) {
            iteration++;

		/* First, compute boundary red values using a standard */
		/* jacobi iteration                                    */
		/* Do southern border */
            for (i = 1, j = (red_corner ? 1 : 2); j <= Np; j += 2) {
                X[i][j] = 0.25 * (B[i][j] + X[i - 1][j] + X[i + 1][j]
                        + X[i][j - 1] + X[i][j + 1]);
            }

		/* Do northern border */
            i = Np;
            if (red_corner) {
                if ((i & 0x1) != 0) j = 1;
                else j = 2;
            } else {
                if ((i & 0x1) != 0) j = 2;
                else j = 1;
            }
            for (; j <= Np; j += 2) {
                X[i][j] = 0.25 * (B[i][j] + X[i - 1][j] + X[i + 1][j]
                        + X[i][j - 1] + X[i][j + 1]);
            }

		/* Do western border */
            for (j = 1, i = (red_corner ? 3 : 2); i < Np; i += 2) {
                X[i][j] = 0.25 * (B[i][j] + X[i - 1][j] + X[i + 1][j]
                        + X[i][j - 1] + X[i][j + 1]);
            }

		/* Do eastern border */
            j = Np;
            if (red_corner) {
                if ((Np & 0x1) != 0) i = 3;
                else i = 2;
            } else {
                if ((Np & 0x1) != 0) i = 2;
                else i = 3;
            }
            for (; i < Np; i += 2) {
                X[i][j] = 0.25 * (B[i][j] + X[i - 1][j] + X[i + 1][j]
                        + X[i][j - 1] + X[i][j + 1]);
            }

		/* Next, send out red borders */
            send_red_borders(X);

		/* Compute interior red points while border values are */
		/* in transit                                          */
            for (i = 2; i < Np; i++) {
                if (red_corner) {
                    if ((i & 0x1) != 0) j = 3;
                    else j = 2;
                } else {
                    if ((i & 0x1) != 0) j = 2;
                    else j = 3;
                }
                for (; j < Np; j += 2) {
                    X[i][j] = 0.25 * (B[i][j] + X[i - 1][j] + X[i + 1][j]
                            + X[i][j - 1] + X[i][j + 1]);
                }
            }

		/* Receive red borders from neighboring processes */
            recv_red_borders(X);

		/* Do border black points using a standard jacobi iteration */
		/* Do southern border */
            for (i = 1, j = (red_corner ? 2 : 1); j <= Np; j += 2) {
                X[i][j] = 0.25 * (B[i][j] + X[i - 1][j] + X[i + 1][j]
                        + X[i][j - 1] + X[i][j + 1]);
            }

		/* Do northern border */
            i = Np;
            if (red_corner) {
                if ((Np & 0x1) != 0) j = 2;
                else j = 1;
            } else {
                if ((Np & 0x1) != 0) j = 1;
                else j = 2;
            }
            for (; j <= Np; j += 2) {
                X[i][j] = 0.25 * (B[i][j] + X[i - 1][j] + X[i + 1][j]
                        + X[i][j - 1] + X[i][j + 1]);
            }

		/* Do western border */
            for (j = 1, i = (red_corner ? 2 : 3); i < Np; i += 2) {
                X[i][j] = 0.25 * (B[i][j] + X[i - 1][j] + X[i + 1][j]
                        + X[i][j - 1] + X[i][j + 1]);
            }

		/* Do eastern border */
            j = Np;
            if (red_corner) {
                if ((Np & 0x1) != 0) i = 2;
                else i = 3;
            } else {
                if ((Np & 0x1) != 0) i = 3;
                else i = 2;
            }
            for (; i < Np; i += 2) {
                X[i][j] = 0.25 * (B[i][j] + X[i - 1][j] + X[i + 1][j]
                        + X[i][j - 1] + X[i][j + 1]);
            }
		/* Send out black borders */
            send_black_borders(X);

		/* While waiting for the black borders to come in, do */
		/* the black interior points.                         */
            for (i = 2; i < Np; i++) {
                if (red_corner) {
                    if ((i & 0x1) != 0) j = 2;
                    else j = 3;
                } else {
                    if ((i & 0x1) != 0) j = 3;
                    else j = 2;
                }
                for (; j < Np; j += 2) {
                    X[i][j] = 0.25 * (B[i][j] + X[i - 1][j] + X[i + 1][j]
                            + X[i][j - 1] + X[i][j + 1]);
                }
            }
		/* Check for termination */
            if (halt_iteration()) return;

		/* Receive black borders */
            recv_black_borders(X);
        }
    }

    static void init_unknowns()
            throws JPVMException {
        int i, j;

	/* Initialize the vector sub-grids */
        X = new double[Np + 2][];
        B = new double[Np + 2][];
        for (i = 0; i < (Np + 2); i++) {
            X[i] = new double[Np + 2];
            B[i] = new double[Np + 2];
            for (j = 0; j < (Np + 2); j++) {
                X[i][j] = 1.0;
                B[i][j] = 0.0;
            }
        }

	/* Ensure that outer boundary values are initialized to zero */
        if (north_id == NOBODY)
            for (i = 0; i < Np + 2; i++) X[Np + 1][i] = 0.0;
        if (south_id == NOBODY)
            for (i = 0; i < Np + 2; i++) X[0][i] = 0.0;
        if (east_id == NOBODY)
            for (i = 0; i < Np + 2; i++) X[i][Np + 1] = 0.0;
        if (west_id == NOBODY)
            for (i = 0; i < Np + 2; i++) X[i][0] = 0.0;

        tmpBorder = new double[Np + 2];
    }

    static boolean halt_iteration()
            throws JPVMException {
        double error_sqr, error_norm;
        double max_error_sqr = max_err * max_err;
        double now;

        if ((max_iterations > 0) && (iteration >= max_iterations))
            return true;

        error_sqr = parallel_dot_prod(X, X);
        System.out.println("|X| = " + error_sqr);
        if (error_sqr <= max_error_sqr) {
            error_norm = Math.sqrt(error_sqr);
            return true;
        }
        return false;
    }

    static double parallel_dot_prod(double x[][], double y[][])
            throws JPVMException {
        int i, j;
        double sum = 0.0;
        double partial_sum;

	/* Returns the dot product x*y */

	/* Calculate the partial result for this process */
        for (i = 1; i <= Np; i++)
            for (j = 1; j <= Np; j++)
                sum += x[i][j] * y[i][j];

        if (my_id == 0) {
		/* Zero gathers then broadcasts the results */
            for (i = 1; i < nprocs; i++) {
                warning("Blocking for " + DOTPROD_TAG);
                Message m = jpvm.pvm_recv(DOTPROD_TAG);
                partial_sum = m.buffer.upkdouble();
                sum += partial_sum;
            }
            Buffer buf = new Buffer();
            buf.pack(sum);
            jpvm.pvm_mcast(buf, childTids, nprocs - 1, DOTPROD_TAG);
        } else {
		/* Others forward their partial results to zero, then */
		/* wait for the complete result                       */
            Buffer buf = new Buffer();
            buf.pack(sum);
            jpvm.pvm_send(buf, tids[0], DOTPROD_TAG);
            warning("Blocking for " + DOTPROD_TAG);
            Message m = jpvm.pvm_recv(DOTPROD_TAG);
            sum = m.buffer.upkdouble();
        }
        return sum;
    }

    static void send_red_borders(double x[][])
            throws JPVMException {
        int i;
        int how_many;
        int st;
        Buffer buf;

        warning("Sending red borders");
        if (north_id != NOBODY) {
		/* Determine the first northern red location */
            how_many = Np / 2;
            if (red_corner) {
                if ((Np & 0x1) != 0) {
                    st = 1;
                    how_many++;
                } else st = 2;
            } else if ((Np & 0x1) != 0) st = 2;
            else st = 1;

		/* Send out northern border */
            for (i = 0; i < how_many; i++)
                tmpBorder[i] = x[Np][st + (i * 2)];
            buf = new Buffer();
            buf.pack(tmpBorder, how_many, 1);
            warning("Sending " + RED_SOUTH_TAG + " to " + tids[north_id]);
            jpvm.pvm_send(buf, tids[north_id], RED_SOUTH_TAG);
        }

        if (south_id != NOBODY) {
		/* Determine the first southern red location */
            how_many = Np / 2;
            if (red_corner) {
                st = 1;
                if ((Np & 0x1) != 0) how_many++;
            } else st = 2;

		/* Send out southern border */
            for (i = 0; i < how_many; i++)
                tmpBorder[i] = x[1][st + (i * 2)];
            buf = new Buffer();
            buf.pack(tmpBorder, how_many, 1);
            warning("Sending " + RED_NORTH_TAG + " to " + tids[south_id]);
            jpvm.pvm_send(buf, tids[south_id], RED_NORTH_TAG);
        }

        if (west_id != NOBODY) {
		/* Determine the first western location */
            how_many = Np / 2;
            if (red_corner) {
                st = 1;
                if ((Np & 0x1) != 0) how_many++;
            } else st = 2;

		/* Send out western border */
            for (i = 0; i < how_many; i++)
                tmpBorder[i] = x[st + (i * 2)][1];
            buf = new Buffer();
            buf.pack(tmpBorder, how_many, 1);
            warning("Sending " + RED_EAST_TAG + " to " + tids[west_id]);
            jpvm.pvm_send(buf, tids[west_id], RED_EAST_TAG);
        }

        if (east_id != NOBODY) {
		/* Determine first eastern location */
            how_many = Np / 2;
            if (red_corner) {
                if ((Np & 0x1) != 0) {
                    st = 1;
                    how_many++;
                } else st = 2;
            } else {
                if ((Np & 0x1) != 0) st = 2;
                else st = 1;
            }

		/* Send out eastern border */
            for (i = 0; i < how_many; i++)
                tmpBorder[i] = x[st + (i * 2)][Np];
            buf = new Buffer();
            buf.pack(tmpBorder, how_many, 1);
            warning("Sending " + RED_WEST_TAG + " to " + tids[east_id]);
            jpvm.pvm_send(buf, tids[east_id], RED_WEST_TAG);
        }
    }

    static void recv_red_borders(double x[][])
            throws JPVMException {
        int i;
        int how_many;
        int st;
        Message m;

        warning("Receiving red borders");
        if (north_id != NOBODY) {
		/* Determine the first northern red location */
            how_many = Np / 2;
            if (red_corner) {
                if ((Np & 0x1) != 0) st = 2;
                else st = 1;
            } else {
                if ((Np & 0x1) != 0) {
                    st = 1;
                    how_many++;
                } else st = 2;
            }
            warning("Blocking for " + RED_NORTH_TAG + " from " + tids[north_id]);
            m = jpvm.pvm_recv(tids[north_id], RED_NORTH_TAG);
            m.buffer.unpack(tmpBorder, how_many, 1);
            for (i = 0; i < how_many; i++)
                x[Np + 1][st + (i * 2)] = tmpBorder[i];
        } else {
            if (iteration <= 1)
                for (i = 0; i < (Np + 2); i++) x[Np + 1][i] = 0.0;
        }

        if (south_id != NOBODY) {
		/* Determine the first southern red location */
            how_many = Np / 2;
            if (red_corner) st = 2;
            else {
                st = 1;
                if ((Np & 0x1) != 0) how_many++;
            }

		/* Receive southern border */
            warning("Blocking for " + RED_SOUTH_TAG + " from " + tids[south_id]);
            m = jpvm.pvm_recv(tids[south_id], RED_SOUTH_TAG);
            m.buffer.unpack(tmpBorder, how_many, 1);
            for (i = 0; i < how_many; i++)
                x[0][st + (i * 2)] = tmpBorder[i];
        } else {
            if (iteration <= 1)
                for (i = 0; i < (Np + 2); i++) x[0][i] = 0.0;
        }

        if (west_id != NOBODY) {
		/* Determine first western receive location */
            how_many = Np / 2;
            if (red_corner) st = 2;
            else {
                st = 1;
                if ((Np & 0x1) != 0) how_many++;
            }
		/* Receive western border */
            warning("Blocking for " + RED_WEST_TAG + " from " + tids[west_id]);
            m = jpvm.pvm_recv(tids[west_id], RED_WEST_TAG);
            m.buffer.unpack(tmpBorder, how_many, 1);
            for (i = 0; i < how_many; i++)
                x[st + (i * 2)][0] = tmpBorder[i];
        } else {
            if (iteration <= 1)
                for (i = 0; i < (Np + 2); i++) x[i][0] = 0.0;
        }

        if (east_id != NOBODY) {
		/* Determine first eastern recv location */
            how_many = Np / 2;
            if (red_corner) {
                if ((Np & 0x1) != 0) st = 2;
                else st = 1;
            } else {
                if ((Np & 0x1) != 0) {
                    st = 1;
                    how_many++;
                } else st = 2;
            }
		/* Receive eastern border */
            warning("Blocking for " + RED_EAST_TAG + " from " + tids[east_id]);
            m = jpvm.pvm_recv(tids[east_id], RED_EAST_TAG);
            m.buffer.unpack(tmpBorder, how_many, 1);
            for (i = 0; i < how_many; i++)
                x[st + (i * 2)][Np + 1] = tmpBorder[i];
        } else {
            if (iteration <= 1)
                for (i = 0; i < (Np + 2); i++) x[i][Np + 1] = 0.0;
        }
    }

    static void send_black_borders(double x[][])
            throws JPVMException {
        int i;
        int how_many;
        int st;
        Buffer buf;

        warning("Sending black borders");
        if (north_id != NOBODY) {
		/* Determine location of first northern black value */
            how_many = Np / 2;
            if (red_corner) {
                if ((Np & 0x1) != 0) st = 2;
                else st = 1;
            } else {
                if ((Np & 0x1) != 0) {
                    st = 1;
                    how_many++;
                } else st = 2;
            }
            for (i = 0; i < how_many; i++)
                tmpBorder[i] = x[Np][st + (i * 2)];
            buf = new Buffer();
            buf.pack(tmpBorder, how_many, 1);
            warning("Sending " + BLACK_SOUTH_TAG + " to " + tids[north_id]);
            jpvm.pvm_send(buf, tids[north_id], BLACK_SOUTH_TAG);
        }

        if (south_id != NOBODY) {
		/* Determine location of first southern black value */
            how_many = Np / 2;
            if (red_corner) st = 2;
            else {
                st = 1;
                if ((Np & 0x1) != 0) how_many++;
            }
            for (i = 0; i < how_many; i++)
                tmpBorder[i] = x[1][st + (i * 2)];
            buf = new Buffer();
            buf.pack(tmpBorder, how_many, 1);
            warning("Sending " + BLACK_NORTH_TAG + " to " + tids[south_id]);
            jpvm.pvm_send(buf, tids[south_id], BLACK_NORTH_TAG);
        }

        if (west_id != NOBODY) {
		/* Determine starting location of western black values */
            how_many = Np / 2;
            if (red_corner) {
                st = 2;
            } else {
                st = 1;
                if ((Np & 0x1) != 0) how_many++;
            }
		/* Send out western border */
            for (i = 0; i < how_many; i++)
                tmpBorder[i] = x[st + (i * 2)][1];
            buf = new Buffer();
            buf.pack(tmpBorder, how_many, 1);
            warning("Sending " + BLACK_EAST_TAG + " to " + tids[west_id]);
            jpvm.pvm_send(buf, tids[west_id], BLACK_EAST_TAG);
        }

        if (east_id != NOBODY) {
		/* Determine eastern starting black location */
            how_many = Np / 2;
            if (red_corner) {
                if ((Np & 0x1) != 0) st = 2;
                else st = 1;
            } else {
                if ((Np & 0x1) != 0) {
                    st = 1;
                    how_many++;
                } else st = 2;
            }
		/* Send out eastern border */
            for (i = 0; i < how_many; i++)
                tmpBorder[i] = x[st + (i * 2)][Np];
            buf = new Buffer();
            buf.pack(tmpBorder, how_many, 1);
            warning("Sending " + BLACK_WEST_TAG + " to " + tids[east_id]);
            jpvm.pvm_send(buf, tids[east_id], BLACK_WEST_TAG);
        }
    }

    static void recv_black_borders(double x[][])
            throws JPVMException {
        int i;
        int how_many;
        int st;
        Message m;

        warning("Receiving black borders");

        if (north_id != NOBODY) {
		/* Determine location of first northern black value to recv */
            how_many = Np / 2;
            if (red_corner) {
                if ((Np & 0x1) != 0) {
                    st = 1;
                    how_many++;
                } else st = 2;
            } else {
                if ((Np & 0x1) != 0) st = 2;
                else st = 1;
            }
		/* Receive northern border */
            warning("Blocking for " + BLACK_NORTH_TAG
                    + " from " + tids[north_id]);
            m = jpvm.pvm_recv(tids[north_id], BLACK_NORTH_TAG);
            m.buffer.unpack(tmpBorder, how_many, 1);
            for (i = 0; i < how_many; i++)
                x[Np + 1][st + (i * 2)] = tmpBorder[i];
        } else {
            if (iteration <= 1)
                for (i = 0; i < (Np + 2); i++) x[Np + 1][i] = 0.0;
        }

        if (south_id != NOBODY) {
		/* Determine first southern black recv location */
            how_many = Np / 2;
            if (red_corner) {
                st = 1;
                if ((Np & 0x1) != 0) how_many++;
            } else st = 2;
		/* Receive southern border */
            warning("Blocking for " + BLACK_SOUTH_TAG + " from "
                    + tids[south_id]);
            m = jpvm.pvm_recv(tids[south_id], BLACK_SOUTH_TAG);
            m.buffer.unpack(tmpBorder, how_many, 1);
            for (i = 0; i < how_many; i++)
                x[0][st + (i * 2)] = tmpBorder[i];
        } else {
            if (iteration <= 1)
                for (i = 0; i < (Np + 2); i++) x[0][i] = 0.0;
        }

        if (west_id != NOBODY) {
		/* Determine starting western black recv location */
            how_many = Np / 2;
            if (red_corner) {
                st = 1;
                if ((Np & 0x1) != 0) how_many++;
            } else st = 2;
		/* Receive western border */
            warning("Blocking for " + BLACK_WEST_TAG + " from " + tids[west_id]);
            m = jpvm.pvm_recv(tids[west_id], BLACK_WEST_TAG);
            m.buffer.unpack(tmpBorder, how_many, 1);
            for (i = 0; i < how_many; i++)
                x[st + (i * 2)][0] = tmpBorder[i];
        } else {
            if (iteration <= 1)
                for (i = 0; i < (Np + 2); i++) x[i][0] = 0.0;
        }

        if (east_id != NOBODY) {
		/* Determine starting western black recv location */
            how_many = Np / 2;
            if (red_corner) {
                if ((Np & 0x1) != 0) {
                    st = 1;
                    how_many++;
                } else st = 2;
            } else {
                if ((Np & 0x1) != 0) st = 2;
                else st = 1;
            }
		/* Receive eastern border */
            warning("Blocking for " + BLACK_EAST_TAG + " from " + tids[east_id]);
            m = jpvm.pvm_recv(tids[east_id], BLACK_EAST_TAG);
            m.buffer.unpack(tmpBorder, how_many, 1);
            for (i = 0; i < how_many; i++)
                x[st + (i * 2)][Np + 1] = tmpBorder[i];
        } else {
            if (iteration <= 1)
                for (i = 0; i < (Np + 2); i++) x[i][Np + 1] = 0.0;
        }
    }

}

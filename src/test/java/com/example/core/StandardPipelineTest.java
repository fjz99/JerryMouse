package com.example.core;

import com.example.Valve;
import com.example.connector.Request;
import com.example.connector.Response;
import com.example.life.LifecycleException;
import com.example.valve.AbstractValve;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import javax.servlet.ServletException;
import java.io.IOException;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertThrows;

class StandardPipelineTest {
    Valve valve1 = new AbstractValve () {
        @Override
        public void invoke(Request request, Response response) throws IOException, ServletException {
            System.out.println ("1");
        }
    };
    Valve valve2 = new AbstractValve () {
        @Override
        public void invoke(Request request, Response response) throws IOException, ServletException {
            System.out.println ("2");
            super.invoke (request, response);
        }
    };
    Valve valve3 = new AbstractValve () {
        @Override
        public void invoke(Request request, Response response) throws IOException, ServletException {
            System.out.println ("3");
            super.invoke (request, response);
        }
    };

    @Test
    void test1() throws ServletException, IOException, LifecycleException {
        StandardPipeline pipeline = new StandardPipeline ();
        pipeline.setBasic (valve1);
        pipeline.start ();
        pipeline.invoke (null, null);
        pipeline.stop ();

        System.out.println ();
        pipeline.addValve (valve2);
        pipeline.addValve (valve3);
        pipeline.start ();
        pipeline.getFirst ().invoke (null, null);
        System.out.println (Arrays.toString (pipeline.getValves ()));
        pipeline.stop ();

        System.out.println ();
        pipeline.removeValve (valve3);
        pipeline.start ();
        pipeline.getFirst ().invoke (null, null);
        pipeline.stop ();

        System.out.println ();
        pipeline.removeValve (valve2);
        pipeline.start ();
        pipeline.getFirst ().invoke (null, null);
        pipeline.stop ();

        System.out.println ();
        pipeline.addValve (valve2);
        pipeline.addValve (valve3);
        pipeline.start ();
        pipeline.getFirst ().invoke (null, null);
        pipeline.stop ();

        System.out.println ();
        pipeline.removeValve (valve2);
        pipeline.start ();
        pipeline.getFirst ().invoke (null, null);
        pipeline.stop ();

        System.out.println ();
        pipeline.removeValve (valve3);
        pipeline.start ();
        pipeline.getFirst ().invoke (null, null);
        pipeline.stop ();

        System.out.println ("新的basic :");
        pipeline.start ();
        pipeline.getFirst ().invoke (null, null);
        pipeline.stop ();
        System.out.print ("新的basic :");
        pipeline.setBasic (new AbstractValve () {
            @Override
            public void invoke(Request request, Response response) {
                System.out.println ("ffffff");
            }
        });
        pipeline.start ();
        pipeline.getFirst ().invoke (null, null);
        pipeline.stop ();
    }

    @Test
    @DisplayName("最后加basic")
    void test() {
        StandardPipeline pipeline = new StandardPipeline ();
        assertThrows (IllegalStateException.class, () -> pipeline.addValve (valve2));
        assertThrows (IllegalStateException.class, () -> pipeline.removeValve (valve2));
        assertThrows (IllegalStateException.class, () -> pipeline.invoke (null, null));
    }

}

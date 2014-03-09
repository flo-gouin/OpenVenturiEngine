package org.ove.incubationbox;

import java.io.File;

import org.eclipse.equinox.app.IApplication;
import org.eclipse.equinox.app.IApplicationContext;
import org.ove.incubationbox.testBox.TIFFReader2;

public class Application implements IApplication {
   public static final String APP_ID = "org.ove.incubationBox";

   @Override
   public Object start(IApplicationContext context) throws Exception {
      System.out.println("Incubation Box Application:");
      TIFFReader2.read(new File("Place file HERE"));
      return null;
   }

   @Override
   public void stop() {

   }

}

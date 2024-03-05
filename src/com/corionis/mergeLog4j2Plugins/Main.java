package com.corionis.mergeLog4j2Plugins;

import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipArchiveInputStream;

import java.io.*;
import java.nio.file.*;

/**
 * MergeLog4j2Plugins
 * <br/>
 * Credit:<br/>
 * A Java "port" of Riccardo Balbo's GitHub gist:<br/>
 *      https://gist.github.com/riccardobl/bdbbae9f2e8fefcd28dc5482ddc6b374
 */
public class Main
{
    public String libDir = "";
    public String log4j2PluginsDatPath = "";
    public final String version = "1.0.0";

    /**
     * Hide default constructor
     */
    private Main()
    {
    }

    /**
     * Main application command line constructor
     */
    public Main(String[] args)
    {
        this.process(args);
    }

    /**
     * main() entry point
     *
     * @param args the input arguments
     */
    public static void main(String[] args)
    {
        new Main(args);
    }

    private int mergeLog4j2PluginsDatTo(InputStream iin, ByteArrayOutputStream to) throws Exception
    {
        int count = 0;
        DataOutputStream writer = new DataOutputStream(to);
        DataInputStream reader = new DataInputStream(iin);
        count = reader.readInt();
        for (int k = 0; k < count; k++)
        {
            writer.writeUTF(reader.readUTF());
            int entries = reader.readInt();
            writer.writeInt(entries);
            for (int j = 0; j < entries; j++)
            {
                writer.writeUTF(reader.readUTF());
                writer.writeUTF(reader.readUTF());
                writer.writeUTF(reader.readUTF());
                writer.writeBoolean(reader.readBoolean());
                writer.writeBoolean(reader.readBoolean());
            }
        }
        writer.flush();
        return count;
    }

    private void parseArgs(String[] args)
    {
        int index;
        for (index = 0; index < args.length; ++index)
        {
            switch (args[index])
            {
                case "-d":
                case "--dat":
                    if (index <= args.length - 1)
                    {
                        log4j2PluginsDatPath = args[index + 1].trim();
                        ++index;
                    }
                    break;
                case "-l":
                case "--lib":
                    if (index <= args.length - 1)
                    {
                        libDir = args[index + 1].trim();
                        ++index;
                    }
                    break;
                default:
                    System.out.println("");
                    System.out.println("Unknown option: " + args[index]);
                    System.out.println("");
                    printHelp();
                    break;
            }
        }
    }

    private void printHelp()
    {
        System.out.println("");
        System.out.println("MergeLog4j2Plugins, version " + version);
        System.out.println("");
        System.out.println("Arguments:");
        System.out.println("  -d | --dat [path] : Path to annotation-generated Log4j2Plugins.dat file.");
        System.out.println("  -l | --lib [path] : Path to project lib directory.");
        System.out.println("");
    }

    public void process(String[] args)
    {
        parseArgs(args);
        if (log4j2PluginsDatPath.length() > 0 && libDir.length() > 0)
        {
            System.out.println("MergeLog4j2Plugins, version " + version);

            File log4j2PluginsDatFile = new File(log4j2PluginsDatPath);
            File parent = log4j2PluginsDatFile.getParentFile();
            ByteArrayOutputStream writer = new ByteArrayOutputStream();

            if (!parent.exists())
            {
                System.out.println("Path $parent does not exists. Create.");
                log4j2PluginsDatFile.getParentFile().mkdirs();
            }

            int count = 0;

            Path libPath = Paths.get(libDir);
            try (DirectoryStream<Path> directoryStream = Files.newDirectoryStream(libPath))
            {
                if (log4j2PluginsDatFile.exists())
                {
                    BufferedInputStream filein = new BufferedInputStream(new FileInputStream(log4j2PluginsDatFile));

                    int num = mergeLog4j2PluginsDatTo(filein, writer);
                    filein.close();
                    System.out.println("  " + num + " entries in " + log4j2PluginsDatFile.toPath().getFileName());
                    count += num;
                }

                for (Path path : directoryStream)
                {
                    if (!path.toString().toLowerCase().endsWith(".jar"))
                        continue;

                    // Apache Commons Compress
                    // https://commons.apache.org/proper/commons-compress
                    ZipArchiveInputStream in = new ZipArchiveInputStream(new BufferedInputStream(Files.newInputStream(path)));
                    ZipArchiveEntry entry = null;
                    while ((entry = in.getNextZipEntry()) != null)
                    {
                        String name = entry.getName();
                        if (name.length() == 0)
                            continue;
                        if (!name.toLowerCase().endsWith("log4j2plugins.dat"))
                            continue;

                        int num = mergeLog4j2PluginsDatTo(in, writer);
                        System.out.println("  " + num + " entries in " + path.getFileName() + " Log4j2Plugins.dat");
                        count += num;
                    }
                    in.close();
                }

                if (count > 0)
                {

                    byte[] bytes = writer.toByteArray();
                    writer.close();

                    DataOutputStream dos = new DataOutputStream(new FileOutputStream(log4j2PluginsDatFile));
                    dos.writeInt(count);
                    dos.write(bytes);
                    dos.close();

                    System.out.println("  Done: " + count + " entries in merged " + log4j2PluginsDatFile.toPath().getFileName());
                }
            }
            catch (Exception e)
            {
                StringWriter sw = new StringWriter();
                PrintWriter pw = new PrintWriter(sw, true);
                e.printStackTrace(pw);
                System.out.println(sw.getBuffer().toString());
            }
        }
        else
            printHelp();
    }

}

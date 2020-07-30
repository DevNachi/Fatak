package dev.allsafeindia.fatak.server;

import android.os.AsyncTask;

import java.io.BufferedOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.Socket;

import dev.allsafeindia.fatak.MainActivity;

public class FileSender extends AsyncTask<Boolean, Void, Boolean> {
    Socket socket;
    File file;
    DataOutputStream dos;
    MainActivity activity;

    public FileSender(Socket socket, File file, MainActivity activity) {
        this.socket = socket;
        this.file = file;
        this.activity = activity;
    }

    @Override
    protected Boolean doInBackground(Boolean... voids) {
        try {
            sentData(file);
            return true;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }

    }

    public void sentData(File file) throws IOException {
        dos = new DataOutputStream(new BufferedOutputStream(socket.getOutputStream()));
        FileInputStream fileInputStream = new FileInputStream(file);
        new File(file.getParent()).mkdir();
        dos.writeUTF(file.getName());
        dos.writeUTF(""+file.length());
        byte[] buffer = new byte[256];
        int i,j = 2;
        int count = 0;
        while ((i = fileInputStream.read(buffer, 0, buffer.length)) != -1&&j>0) {
            dos.write(buffer, 0,i);
            count+=i;
            System.out.println("chunk send "+ count);
            j = fileInputStream.available();
        }
        fileInputStream.close();
        dos.close();
        System.out.println("CLIENT: Data Send Successfully " + count);
        activity.onThreadWorkDone("CLIENT: Data Send Successfully ");
    }

    @Override
    protected void onPostExecute(Boolean aBoolean) {
        super.onPostExecute(aBoolean);
        System.out.println(aBoolean + "OUTPUT");
    }
}

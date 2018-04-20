package com.squidswap.inktag.inktag;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.net.Uri;
import android.provider.MediaStore;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.Toast;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;

public class InktagActivity extends AppCompatActivity {

    private ImageButton AddButton,CheckButton,CancelButton;
    private RelativeLayout MainStage;
    private InkCanvas can;
    private Uri FocusedImage;
    private int SELECT_PICTURE = 1;
    private FileService fs;
    private ArrayList<TextModule> Tags;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_inktag);

        this.Tags = new ArrayList<TextModule>();
        this.fs = new FileService();
        this.MainStage = (RelativeLayout) findViewById(R.id.MainStage);
        this.can = new InkCanvas(getApplicationContext());
        this.MainStage.addView(this.can);
        this.InitializeBottomButtons();

        //Load an image from the gallery.
        Intent intent = new Intent();
        intent.setType("image/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);
        startActivityForResult(Intent.createChooser(intent, "Select Picture"), SELECT_PICTURE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if(requestCode == 1){
            this.FocusedImage = data.getData();
            can.invalidate();
        }
    }

    private void InitializeBottomButtons(){
        this.AddButton = (ImageButton) findViewById(R.id.AddTextButton);

        this.AddButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                LayoutInflater infl = getLayoutInflater();
                LinearLayout r = (LinearLayout) infl.inflate(R.layout.new_text_dialog,null);
                AlertDialog.Builder build = new AlertDialog.Builder(InktagActivity.this);
                build.setView(r);

                build.setTitle("Text Content").setPositiveButton("Confirm", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        Tags.add(new TextModule());
                        can.invalidate();
                    }
                }).setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {

                    }
                }).show();
            }
        });
    }

    private class TextModule{
        private String TextContent = "";
        private float x,y;
        private boolean ShowBorders;
        private Typeface ChosenFont;

        public TextModule(){

        }
    }

    //Class for drawing different things onto the canvas.
    private class InkCanvas extends View{
        private Paint SelectionPaint;

        public InkCanvas(Context context) {
            super(context);

            this.SelectionPaint = new Paint();
            this.SelectionPaint.setColor(Color.WHITE);
            this.SelectionPaint.setStyle(Paint.Style.STROKE);
            this.SelectionPaint.setStrokeWidth(2);
        }

        @Override
        protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);

            if(FocusedImage != null){
                Bitmap b = ScaleImage(fs.LoadBitmapFromUri(FocusedImage));
                canvas.drawBitmap(b,(getWidth() - b.getWidth()) / 2,(getHeight() - b.getHeight()) / 2,null);

                for(int i = 0;i < Tags.size();i++){
                    canvas.drawRect(new Rect(getHeight() - 300,0,getHeight(),getWidth()),this.SelectionPaint);
                }
            }
        }

        private Bitmap ScaleImage(Bitmap b){
            float scale;
            if(b.getWidth() < getWidth()){
                scale = getWidth() / b.getWidth();
            }else{
                scale = 1;
            }

            Matrix m = new Matrix();
            m.setScale(scale,scale);
            Bitmap fin = Bitmap.createBitmap(b,0,0,b.getWidth(),b.getHeight(),m,true);
            return fin;
        }
    }

    private class FileService{
        public FileService(){}

        private Bitmap LoadBitmapFromUri(Uri u){
            Bitmap b;

            try {
                b = MediaStore.Images.Media.getBitmap(getContentResolver(),u);
            } catch (IOException e) {
                b = null;
                e.printStackTrace();
            }

            return b;
        }
    }
}

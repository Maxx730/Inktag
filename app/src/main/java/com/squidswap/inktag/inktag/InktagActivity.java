package com.squidswap.inktag.inktag;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Typeface;
import android.net.Uri;
import android.provider.MediaStore;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
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

        this.fs = new FileService();
        this.MainStage = (RelativeLayout) findViewById(R.id.MainStage);
        this.can = new InkCanvas(getApplicationContext());
        this.MainStage.addView(this.can);

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
        public InkCanvas(Context context) {
            super(context);
        }

        @Override
        protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);

            if(FocusedImage != null){
                canvas.drawBitmap(fs.LoadBitmapFromUri(FocusedImage),0,0,null);
            }
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

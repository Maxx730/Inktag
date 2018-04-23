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
import android.support.annotation.LayoutRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.Layout;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import org.w3c.dom.Text;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class InktagActivity extends AppCompatActivity {

    private ImageButton AddButton,CheckButton,CancelButton,FontChoiceBtn,FontOptionButton;
    private RelativeLayout MainStage;
    private InkCanvas can;
    private Uri FocusedImage;
    private int SELECT_PICTURE = 1,BOX_HEIGHT = 100;
    private FileService fs;
    private ArrayList<TextModule> Tags;
    private ArrayList<FontItem> FontList;
    private TextModule CurrentText;
    private float pointerX,pointerY;
    private ListView fonts;
    private Typeface CurrentTypeFace;
    private AlertDialog.Builder FontChoiceBuild;
    private AlertDialog FontChoiceDia;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_inktag);

        this.Tags = new ArrayList<TextModule>();
        this.FontList = new ArrayList<FontItem>();
        FontChoiceBuild = new AlertDialog.Builder(InktagActivity.this);
        this.FontList.add(new FontItem("AdmiralCAT",getResources().getFont(R.font.admiralcat)));
        this.FontList.add(new FontItem("Intransitive",getResources().getFont(R.font.intransitive)));
        this.FontList.add(new FontItem("Soccer League",getResources().getFont(R.font.soccerleague)));
        this.FontList.add(new FontItem("Turtles Are Cool",getResources().getFont(R.font.turts)));
        this.FontList.add(new FontItem("Foo",getResources().getFont(R.font.foo)));
        this.FontList.add(new FontItem("Tooney",getResources().getFont(R.font.tooney)));

        this.fs = new FileService();
        this.MainStage = (RelativeLayout) findViewById(R.id.MainStage);
        this.can = new InkCanvas(getApplicationContext());
        this.MainStage.addView(this.can);
        this.InitializeBottomButtons();
        this.CurrentText = new TextModule("No Text Available");

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
        this.FontChoiceBtn = (ImageButton) findViewById(R.id.EditFontButton);
        this.FontOptionButton = (ImageButton) findViewById(R.id.FontOptionIcon);

        this.FontOptionButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                AlertDialog.Builder b = new AlertDialog.Builder(InktagActivity.this);
                LayoutInflater infl = getLayoutInflater();
                LinearLayout l = (LinearLayout) infl.inflate(R.layout.font_settings_dialog,null);
                b.setView(l);

                b.setTitle("Font Options").setPositiveButton("Confirm", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {

                    }
                }).setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {

                    }
                }).show();
            }
        });

        this.FontChoiceBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                FontChoiceDia = FontChoiceBuild.create();
                LayoutInflater infl = getLayoutInflater();
                LinearLayout r = (LinearLayout) infl.inflate(R.layout.font_choice_dialog,null);
                fonts = (ListView) r.findViewById(R.id.FontList);
                FontListAdapter ad = new FontListAdapter(getApplicationContext(),FontList);
                fonts.setAdapter(ad);
                FontChoiceBuild.setView(r);

                FontChoiceBuild.setTitle("Choose Font").setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {

                    }
                }).setPositiveButton("Confirm", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {

                    }
                }).show();
            }
        });

        this.AddButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                LayoutInflater infl = getLayoutInflater();
                final LinearLayout r = (LinearLayout) infl.inflate(R.layout.new_text_dialog,null);
                AlertDialog.Builder build = new AlertDialog.Builder(InktagActivity.this);
                final EditText t = (EditText) r.findViewById(R.id.TextContent);
                t.setText(CurrentText.TextContent);
                build.setView(r);

                build.setTitle("Text Content").setPositiveButton("Confirm", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        EditText t = (EditText) r.findViewById(R.id.TextContent);
                        CurrentText.TextContent = t.getText().toString();
                        can.HAS_TEXT = true;
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

        public TextModule(String content){
            this.TextContent = content;
        }
    }

    //Class for drawing different things onto the canvas.
    private class InkCanvas extends View{
        private Paint SelectionPaint,TextPaint;
        private Boolean HAS_TEXT = false;

        public InkCanvas(Context context) {
            super(context);

            this.SelectionPaint = new Paint();
            this.SelectionPaint.setColor(Color.BLACK);
            this.SelectionPaint.setStyle(Paint.Style.FILL);
            this.SelectionPaint.setStrokeWidth(2);

            this.TextPaint = new Paint();
            this.TextPaint.setColor(Color.WHITE);
            this.TextPaint.setStyle(Paint.Style.FILL);
            this.TextPaint.setTextSize(30);
            this.TextPaint.setAntiAlias(true);
        }

        @Override
        public boolean onTouchEvent(MotionEvent event) {

            switch(event.getAction()){
                case MotionEvent.ACTION_DOWN:
                        if(HAS_TEXT){
                            pointerY = event.getY();
                        }
                    break;
                case MotionEvent.ACTION_MOVE:
                        if(HAS_TEXT){
                            pointerY = event.getY();
                        }
                    break;
            }

            invalidate();
            return true;
        }

        @Override
        protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);

            if(FocusedImage != null){
                ViewGroup.MarginLayoutParams p = (ViewGroup.MarginLayoutParams) getLayoutParams();
                Bitmap b = ScaleImage(fs.LoadBitmapFromUri(FocusedImage));
                canvas.drawBitmap(b,(getWidth() - b.getWidth()) / 2,(getHeight() - b.getHeight()) / 2,null);

                if(HAS_TEXT){
                    if(CurrentTypeFace != null){
                        this.TextPaint.setTypeface(CurrentTypeFace);
                    }

                    canvas.drawRect(new Rect((getWidth() - b.getWidth()) / 2,(int) Math.floor(pointerY - this.TextPaint.getTextSize()),((getWidth() - b.getWidth()) / 2) + b.getWidth(), (int) Math.floor(pointerY + this.TextPaint.getTextSize())),this.SelectionPaint);
                    canvas.drawText(CurrentText.TextContent,30,(pointerY + (this.TextPaint.getTextSize() / 2)),this.TextPaint);
                }
            }
        }

        private Bitmap ScaleImage(Bitmap b){
            float scale;

                if(b.getWidth() < getWidth()){
                    scale = (float) Math.floor(getWidth() / b.getWidth());
                }else{
                    scale = b.getWidth() / getWidth();
                }

            Matrix m = new Matrix();
            m.setScale(scale,scale);
            Bitmap fin = Bitmap.createBitmap(b,0,0,b.getWidth(),b.getHeight(),m,true);
            return fin;
        }
    }

    private class FontItem{
        public String fontName;
        public Typeface font;

        public FontItem(String name,Typeface font){
            this.fontName = name;
            this.font = font;
        }
    }

    //Adapter that will take it font objects and apply them to a list view.
    private class FontListAdapter extends ArrayAdapter<FontItem>{

        public FontListAdapter(@NonNull Context context, ArrayList<FontItem> fonts) {
            super(context,0,fonts);
        }

        @NonNull
        @Override
        public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
            View listItem = LayoutInflater.from(getApplicationContext()).inflate(R.layout.font_list_item,parent,false);

            TextView v = listItem.findViewById(R.id.FontOptionText);
            v.setText(FontList.get(position).fontName);
            v.setTextSize(30);
            v.setTextScaleX(1);
            v.setTypeface(FontList.get(position).font);

            final Typeface clickFace = FontList.get(position).font;

            listItem.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    CurrentTypeFace = clickFace;
                    FontChoiceDia.cancel();
                }
            });

            return listItem;
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

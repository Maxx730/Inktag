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
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.jaredrummler.android.colorpicker.ColorPickerDialog;
import com.jaredrummler.android.colorpicker.ColorPickerDialogListener;

import org.w3c.dom.Text;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class InktagActivity extends AppCompatActivity implements ColorPickerDialogListener{

    private ImageButton AddButton,CheckButton,CancelButton,FontChoiceBtn,FontOptionButton;
    private RelativeLayout MainStage;
    private InkCanvas can;
    private Uri FocusedImage;
    private int SELECT_PICTURE = 1,FONT_SIZE = 50;
    private FileService fs;
    private ArrayList<TextModule> Tags;
    private ArrayList<FontItem> FontList;
    private TextModule CurrentText;
    private float pointerX,pointerY;
    private ListView fonts;
    private Typeface CurrentTypeFace;
    private AlertDialog.Builder FontChoiceBuild;
    private AlertDialog FontChoiceDia;
    private SeekBar ZoomSeeker;

    @Override public void onColorSelected(int dialogId, int color) {
        CurrentText.TextColor = color;
        can.invalidate();
    }

    @Override public void onDialogDismissed(int dialogId) {

    }

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
        this.CurrentText = new TextModule("");
        FontChoiceDia = FontChoiceBuild.create();
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
                ColorPickerDialog.newBuilder().show(InktagActivity.this);
            }
        });

        this.FontChoiceBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                LayoutInflater infl = getLayoutInflater();
                LinearLayout r = (LinearLayout) infl.inflate(R.layout.font_choice_dialog,null);
                fonts = (ListView) r.findViewById(R.id.FontList);
                FontListAdapter ad = new FontListAdapter(getApplicationContext(),FontList);
                fonts.setAdapter(ad);
                FontChoiceBuild.setView(r);

                FontChoiceDia = FontChoiceBuild.setTitle("Choose Font").show();
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
        private int TextColor;

        public TextModule(String content){
            this.TextContent = content;
            this.TextColor = Color.WHITE;
        }

        //Function determines the length of string based on the width of the canvas
        //and breaks the string into line segments.
        public ArrayList<String> seperate(int width,Paint textPaint){
            ArrayList<String> seperated = new ArrayList<String>();
            String[] split = CurrentText.TextContent.split(" ");

            //Determine the max length that the words can occupy.
            //I.E it can only take up 80% of the screen.
            float maxWidth = (float) ((float) can.getWidth() * 0.8);
            String currentLine = "";

            for(int i = 0;i < split.length;i++){
                //First we need to use the paint object to determine the length of
                //the currentline plus the length of the next word.
                float paintLength = textPaint.measureText(currentLine + split[i]);
                if(paintLength < maxWidth){
                    currentLine += " " + split[i];

                    if((i + 1) == split.length){
                        seperated.add(currentLine);
                    }
                }else{
                    seperated.add(currentLine);
                    currentLine = "";
                }
            }

            return seperated;
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
            this.TextPaint.setTextSize(FONT_SIZE);
            this.TextPaint.setAntiAlias(true);
            this.TextPaint.setTextAlign(Paint.Align.CENTER);
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


                    if(CurrentTypeFace != null){
                        this.TextPaint.setTypeface(CurrentTypeFace);
                    }

                    ArrayList<String> values = CurrentText.seperate(getWidth(),TextPaint);
                    this.TextPaint.setColor(CurrentText.TextColor);

                    for(int i = 0;i < values.size();i++){
                        canvas.drawText(values.get(i),(getWidth() / 2),(pointerY + (this.TextPaint.getTextSize() / 2)) + (i * this.TextPaint.getTextSize()),this.TextPaint);
                    }
                    //canvas.drawRect(new Rect((getWidth() - b.getWidth()) / 2,(int) Math.floor(pointerY - (this.TextPaint.getTextSize() * 2)),((getWidth() - b.getWidth()) / 2) + b.getWidth(), (int) Math.floor(pointerY + (this.TextPaint.getTextSize() * 2.5))),this.SelectionPaint);

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
            v.setTextColor(Color.WHITE);
            v.setTypeface(FontList.get(position).font);

            final Typeface clickFace = FontList.get(position).font;

            listItem.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    CurrentTypeFace = clickFace;
                    can.invalidate();
                    FontChoiceDia.dismiss();
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

    //Class that will initialize creating an inkslice activity.
    public static class TagBuilder extends Intent {
        private Uri First;
        private Context ctx;

        public TagBuilder(Context ctx,String TempFileName){
            super(ctx,InktagActivity.class);
            TempFileName = TempFileName;
            this.First = Uri.parse(ctx.getApplicationContext().getCacheDir() + "/" + TempFileName);
            this.ctx = ctx;
        }

        //Starts the slicing activity.
        public Intent start(){
            this.putExtra("InkTagFile",this.First);
            return this;
        }
    }

}

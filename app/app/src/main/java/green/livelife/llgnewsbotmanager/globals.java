/*
globals.java
Copyright (C) 2020 Alexander Theulings, ketchupcomputing.com <alexander@theulings.com>

This program is free software: you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program.  If not, see <http://www.gnu.org/licenses/>.

Documentation for this file can be found at https://ketchupcomputing.com/llg-news-bot/
*/

package green.livelife.llgnewsbotmanager;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.util.Log;

import androidx.appcompat.app.AlertDialog;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.Base64;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

import static android.content.Context.MODE_PRIVATE;

public class globals {
    public static final String sharedPrefStr = "green.livelife.llgnewsbotmanager.sp";
    public static final String serviceURL = "https://ketchupcomputing.com/llgNewsBotProvider.php";
    public static final String errorString = "Error - View Logcat.";

    static final MediaType typeJson = MediaType.parse("application/json; charset=utf-8");
    static final OkHttpClient client = new OkHttpClient();

    private static Context currentContext;
    private static String warn;

    static String postToProvider(String json) throws IOException {
        String result = "Connection error?";
        RequestBody body = RequestBody.create(json, typeJson);
        Request request = new Request.Builder()
                .url(serviceURL)
                .post(body)
                .build();
        Response response = client.newCall(request).execute();
        result = response.body().string();
        return result;
    }

    static JSONObject newJson(int reqType) throws JSONException {
        JSONObject json = new JSONObject();
        SharedPreferences sp = currentContext.getSharedPreferences(sharedPrefStr, MODE_PRIVATE);
        String uname = sp.getString("u", "");
        String pword = sp.getString("p", "");
        json.put("username", new String(Base64.getEncoder().encode(uname.getBytes())));
        json.put("password", new String(Base64.getEncoder().encode(pword.getBytes())));
        json.put("requestType", reqType);
        return json;
    }

    static void displayWarning(Context cont){
        if (!warn.equals("")) {
            AlertDialog alertDialog = new AlertDialog.Builder(cont).create();
            alertDialog.setTitle("Warning");
            alertDialog.setMessage(warn);
            alertDialog.show();
            warn = "";
        }
    }

    public static class testLogin extends AsyncTask<Context, Void, String> {
        @Override
        protected String doInBackground(Context... params) {
            currentContext = params[0];
            try{
                JSONObject json = newJson(1);
                return postToProvider(json.toString());
            } catch (JSONException e) {
                Log.e("JSON Error", e.toString());
            } catch (IOException e) {
                Log.e("IO Error", e.toString());
                return e.getMessage();
            }
            return errorString;
        }

        @Override
        protected void onPostExecute(String result) {
            if (result.equals("loginSuccess")) {
                Intent intent = new Intent(currentContext, MainActivity.class);
                //Clear the back stack so the back button closes the manager rather than going back to this login screen
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                currentContext.startActivity(intent);
                ((Activity)currentContext).finish();
            } else {
                if (currentContext.getClass().getSimpleName().equals("LoginActivity")) {
                    warn = result;
                    displayWarning(currentContext);
                }else {
                    warn = result;
                    Intent intent = new Intent(currentContext, LoginActivity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                    currentContext.startActivity(intent);
                    ((Activity) currentContext).finish();
                }
            }
        }
    }
}

package fpmbuild.agent;

import fpmbuild.Main;
import fpmbuild.json.Json;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public final class AgentServer {
    public void run() throws IOException {
        BufferedReader in=new BufferedReader(new InputStreamReader(System.in,StandardCharsets.UTF_8));
        PrintWriter out=new PrintWriter(System.out,true,StandardCharsets.UTF_8);
        String line;
        while((line=in.readLine())!=null){
            if(line.isBlank())continue;
            try{
                Map<String,Object> req=Json.asObject(Json.parse(line));
                Object id=req.get("id"); String command=String.valueOf(req.getOrDefault("command","capabilities"));
                List<String> args=new ArrayList<>();args.add(command);
                Object a=req.get("args");if(a instanceof List<?> list)for(Object x:list)args.add(String.valueOf(x));
                Object params=req.get("params");
                if(params instanceof Map<?,?> map){
                    for(var e:map.entrySet()){
                        String key="--"+e.getKey(); Object value=e.getValue();
                        if(value instanceof Boolean b){ if(b) args.add(key); }
                        else if(value instanceof List<?> values){ for(Object v:values){args.add(key);args.add(String.valueOf(v));} }
                        else if(value!=null){args.add(key);args.add(String.valueOf(value));}
                    }
                }
                Map<String,Object> result=Main.executeForAgent(args.toArray(String[]::new));
                out.println(Json.stringify(Json.object("id",id,"ok",true,"result",result)));
                if(command.equals("shutdown"))break;
            }catch(Exception e){out.println(Json.stringify(Json.object("ok",false,"error",e.getClass().getSimpleName(),"message",String.valueOf(e.getMessage()))));}
        }
    }
}

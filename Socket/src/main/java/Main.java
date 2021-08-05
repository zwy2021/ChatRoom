// 本题为考试单行多行输入输出规范示例，无需提交，不计分。
import java.util.*;
public class Main {
    int res=0;
    public static void main(String[] args) {
        Scanner in = new Scanner(System.in);
        do{
            int n=in.nextInt();
            int m=in.nextInt();
            int k=in.nextInt();
            int x=in.nextInt();
            int[] nums= new int[n];
            for (int i = 0; i < n; i++) {
                nums[i]=in.nextInt();
            }
            boolean[] visited = new boolean[n];
            Main main = new Main();
            main.dfs(nums,0,visited,m,0,x,k);
            System.out.print(main.res);
        }while(in.hasNext());

    }
    void dfs(int[] nums,int path,boolean[] visited,int m,int digit,int x,int k){
        if(digit==m&&path%x==k){
            res++;
            return;
        }
        int n=nums.length;
        for (int i = 0; i < n; i++) {
            if(digit!=0&&(path%10+nums[i])%2==0){
                continue;
            }
            if(visited[i]==false){
                visited[i]=true;
                digit++;
                path=path*10+nums[i];
                dfs(nums,path,visited,m,digit,x,k);
                visited[i]=false;
                digit--;
                path/=10;
            }

        }
    }
}
package com.lc.yunpicturebackend.utils.picture;

import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.lc.yunpicturebackend.exception.BusinessException;
import com.lc.yunpicturebackend.exception.ErrorCode;
import lombok.extern.slf4j.Slf4j;

/**
 * 对cos格式的颜色转化为标准的十六进制格式
 */
@Slf4j
public class ColorTransformUtils {

    private ColorTransformUtils() {
        // 工具类不需要实例化
    }

    /**
     * 对该算法进行分析
     * e0020转换为e00200 还是 e00020：
     * 在COS中，会有前导0的情况，前面两位 和 中间两位 和 最后两位 互不相干，
     * <p>
     * 所以，我们要分开看：
     * 对于前导0，由于0在高位时对低位的数字毫无影响，所以可以省略，而在低位的0不能省略。
     * <p>
     * 分析：
     * e0020就是这样，第一位是e，e在前两位的高位，所以他是没有省略的，所以在完整的写法中前两位肯定是e0。
     * 那么对剩下的020进行分析：0在高位，如果说02是中间两位那么这个0显然可以省略，即02->2，但是与题设矛盾
     * 所以020实际上应该是：0020，
     * 最终得出：结果e0020->e00020。
     * <p>
     * 继续分析其它例子，
     * 比如0c00，看前两位0c，如果0c是RR，那么0c可以省略为c，但是前两位是0c，说明0c应该是00c，所以前两位是00
     * 中间两位是c0，现在剩最后一位0，补全为00，所以完整写法是00c000。
     *
     * @param rawColor 未经校准的十六进制格式的颜色
     * @return 标准的十六进制格式的颜色
     */
    //todo 看能不能进行优化
    public static String getStandardColor(String rawColor) {
        //前端传递的格式可能是#aabbcc
        if(rawColor.startsWith("#")){
            rawColor = rawColor.replace("#", "0x");
        }
        if(StringUtils.isBlank(rawColor) || !rawColor.startsWith("0x")){
            throw new BusinessException(ErrorCode.PARAMS_ERROR,"颜色为空或格式不正确");
        }
        //去除0x后的字符串
        String hexString = rawColor.substring(2);
        String r = "00";
        String g = "00";
        String b = "00";
        //根据hexString的长度进行枚举,分类讨论 6,5,4,3
        if(hexString.length()==6){
            return "0x"+hexString;
        }
        //
        if(hexString.length()==5){
            // 例子:e0020 高位0可以省略
            //rrggbb
            if(hexString.startsWith("0")){
                //说明rr省略了
                return "0x"+"0"+hexString;
            }else{
                //说明是gg 和 bb 省略了一个0
                String redString = hexString.substring(0, 2);
                String restString = hexString.substring(2);
                if(restString.startsWith("0")){
                    //说明gg省略了
                    return "0x"+redString+ "0"+restString;
                }else {
                    //说明bb省略了
                    String greenString = restString.substring(0, 2);
                    String blueString = restString.substring(2);
                    return "0x"+ redString + greenString + "0" + blueString;
                }
            }
        }
        if(hexString.length()==4){
            //例子:0c00  实际是00c000 高位0可以省略
            if(hexString.startsWith("0")){
                //说明rr缺了一个0
                String redString = hexString.substring(0,1);
                String restString = hexString.substring(1);
                if(restString.startsWith("0")){
                    //说明gg缺了一个0
                    String greenString = restString.substring(0, 1);
                    String blueString = restString.substring(1);
                    return "0x" + "0" +redString + "0" +greenString + blueString;
                }else {
                    //说明bb缺了一个0
                    String greenString = restString.substring(0, 2);
                    String blueString = restString.substring(2);
                    return "0x" + "0" +redString + greenString + "0" + blueString;
                }
            }else{
                //说明是gg和bb缺了 0
                String redString = hexString.substring(0,2);
                String greenString = hexString.substring(2,3);
                String blueString = hexString.substring(3);
                return "0x" + redString + "0" + greenString + "0" + blueString;
            }
        }
        if(hexString.length()==3){
            //相当于rgb的首位都缺了0
            String redString = "0" + hexString.charAt(0);
            String greenString = "0" + hexString.charAt(1);
            String blueString = "0" + hexString.charAt(2);
            return "0x" + redString + greenString + blueString;
        }
        //如果走到以下代码,说明传递的颜色格式有误
        throw new BusinessException(ErrorCode.PARAMS_ERROR,"无法解析正确结果,颜色格式有误");
    }


    /**
     * 根据图片主色调计算 相似度
     * @param color1 十六进制格式
     * @param color2 十六进制格式
     * @return value 返回的值越大,说明越相似
     */
    public static double getSimilarityOfPictureByColor(String color1, String color2) {
        if(color1 == null || color2 == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR,"颜色不能为空");
        }
        String standardColor1 = getStandardColor(color1);
        String standardColor2 = getStandardColor(color2);
        //去除掉0x后的字符串
        String substring1 = standardColor1.substring(2);
        String substring2 = standardColor2.substring(2);

        double distance = 0;
        for(int i=0;i<6;i=i+2){
            String hexColor1 = substring1.substring(i, i + 2);
            String hexColor2 = substring2.substring(i, i + 2);
            int decimalColor1 = Integer.parseInt(hexColor1, 16);
            int decimalColor2 = Integer.parseInt(hexColor2, 16);
            distance += ((Math.pow((decimalColor1-decimalColor2),2)));
        }
        distance = Math.sqrt(distance);
        //欧几里得距离
        return 1 - distance / Math.sqrt(3*Math.pow(255,2));
    }

    public static void main(String[] args) {
        //value 值越大,说明越相似
        double value = getSimilarityOfPictureByColor("0xff0000", "0xee1122");
        System.out.println(value);

    }
}

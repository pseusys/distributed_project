package ds.misc;

import java.util.ArrayList;
import java.util.List;


public class Utils {
    public static List<int[]> permute(int[] nums) {
        List<int[]> result = new ArrayList<>();
        Utils.permute_internal(0, nums, result);
        return result;
    }

    private static void permute_internal(int i, int[] nums, List<int[]> result) {
        if (i == nums.length - 1) result.add(nums.clone());
        else for (int j = i, l = nums.length; j < l; j++) {
            int temp = nums[j];
            nums[j] = nums[i];
            nums[i] = temp;
            permute_internal(i + 1, nums, result);
            temp = nums[j];
            nums[j] = nums[i];
            nums[i] = temp;
        }
    }
}

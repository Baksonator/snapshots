package servent.message.util;

import app.AppConfig;
import app.snapshot_bitcake.LYSnapshotResult;
import servent.message.Message;
import servent.message.snapshot.LYTellMessage;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class ChildResultCollector implements Runnable {

    private final int neighborCount;
    private final LYSnapshotResult myResult;
    private final int parent;

    public ChildResultCollector(int neighborCount, LYSnapshotResult myResult, int parent) {
        this.neighborCount = neighborCount;
        this.myResult = myResult;
        this.parent = parent;
    }

    @Override
    public void run() {
        int children = 0;
        Set<Integer> neighobringRegions = new HashSet<>();

        int i = 0;
        while (i < neighborCount) {
            try {
                int res = AppConfig.neighborResponses.take();
                if (res == -1) {
                    children++;
                } else if (res >= 0) {
                    neighobringRegions.add(res);
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            i++;
        }

        List<LYSnapshotResult> lySnapshotResults = new ArrayList<>();
        while (children > 0) {
            try {
                lySnapshotResults.addAll(AppConfig.childrenResponses.take());
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            children--;
        }

        myResult.setNeighboringRegions(neighobringRegions);
        lySnapshotResults.add(myResult);

        Message tellMessage = new LYTellMessage(
                AppConfig.myServentInfo, AppConfig.getInfoById(parent), lySnapshotResults);

        AppConfig.timestampedStandardPrint("My parent in tree is " + parent);

        MessageUtil.sendMessage(tellMessage);

//        AppConfig.treeParent.set(-1);
//        AppConfig.region.set(-1);
    }
}

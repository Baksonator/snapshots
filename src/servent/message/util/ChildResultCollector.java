package servent.message.util;

import app.AppConfig;
import app.snapshot_bitcake.LYSnapshotResult;
import servent.message.Message;
import servent.message.snapshot.LYTellMessage;

import java.util.*;

public class ChildResultCollector implements Runnable {

    private final int neighborCount;
    private final LYSnapshotResult myResult;
    private final int parent;
    private final Map<Integer, LYSnapshotResult> allMyLySnapshotResults;

    public ChildResultCollector(int neighborCount, LYSnapshotResult myResult, int parent, Map<Integer,
            LYSnapshotResult> allMyLySnapshotResults) {
        this.neighborCount = neighborCount;
        this.myResult = myResult;
        this.parent = parent;
        this.allMyLySnapshotResults = allMyLySnapshotResults;
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

        Map<Integer, List<LYSnapshotResult>> allLySnapshotResults = new HashMap<>();
        for (int initiator : AppConfig.initiatorIds) {
            allLySnapshotResults.put(initiator, new ArrayList<>());
        }

        List<LYSnapshotResult> lySnapshotResults = new ArrayList<>();
        while (children > 0) {
            try {
                lySnapshotResults.addAll(AppConfig.childrenResponses.take());

                Map<Integer, List<LYSnapshotResult>> oneMapResult = AppConfig.childrenResponsesAlt.take();
                for (Map.Entry<Integer, List<LYSnapshotResult>> entry : oneMapResult.entrySet()) {
                    allLySnapshotResults.get(entry.getKey()).addAll(oneMapResult.get(entry.getKey()));
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            children--;
        }

        myResult.setNeighboringRegions(neighobringRegions);
        lySnapshotResults.add(myResult);
        for (int initiator : AppConfig.initiatorIds) {
            allLySnapshotResults.get(initiator).add(allMyLySnapshotResults.get(initiator));
        }

        Message tellMessage = new LYTellMessage(
                AppConfig.myServentInfo, AppConfig.getInfoById(parent), lySnapshotResults, allLySnapshotResults);

        AppConfig.timestampedStandardPrint("My parent in tree is " + parent);

        MessageUtil.sendMessage(tellMessage);

//        AppConfig.treeParent.set(-1);
//        AppConfig.region.set(-1);
    }
}

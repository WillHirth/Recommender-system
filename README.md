# Recommender-system
Recommender systems coursework for the module Social Computing Techniques (COMP3208) at the University of Southampton. Final mark for the module - 80%.

Task 1 - Calculate the MSE, RMSE, and MAE of the dataset.
Task 2 - Implement a cosine similarity algorithm to predict a users rating for a given item in the 100k dataset.
Task 3 - Implement a matrix factorisation algorithm to predict a users rating for a given item in the 100k dataset.
Task 4 - Implement a matrix factorisation algorithm to predict a users rating for a given item in the 20M dataset.

Task 1 format:
Output format: MSE (float), RMSE (float), MAE (float)
Comment: the provided micro ratings and predictions should be used to compute these eval metrics

Task 2, 3, and 4 format:
Training data format: user_id (int), item_id (int), rating (float), timestamp (int)
Input format: user_id (int), item_id (int), timestamp (int)

Output format: user_id (int), item_id (int), rating_prediction (float), timestamp (int)
Comment: the user_id, item_id and timestamp is taken from the 100k or 20M testset and you provide the predicted_rating (value from 0.5 to 5.0) 
